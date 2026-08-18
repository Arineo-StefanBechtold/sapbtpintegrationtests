package me.cxdev.testing.cpi.extension;

import java.util.List;
import java.util.stream.Collectors;

import me.cxdev.testing.cpi.collector.CollectorClient;
import me.cxdev.testing.cpi.config.ConfigLoader;
import me.cxdev.testing.cpi.config.CpiTestConfig;
import me.cxdev.testing.cpi.dsl.CpiTestDsl;
import me.cxdev.testing.cpi.http.OkHttpTransport;
import me.cxdev.testing.cpi.ledger.Ledger;
import me.cxdev.testing.cpi.monitoring.CpiMonitoringClient;
import me.cxdev.testing.cpi.residual.ResidualChecker;
import me.cxdev.testing.cpi.residual.ResidualDocument;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class CpiTestExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(CpiTestExtension.class);
    private static final String CONTEXT_RESOURCE_KEY = "cpi-test-context-resource";

    @Override
    public void beforeAll(ExtensionContext context) {
        ContextResource resource = context.getRoot().getStore(NAMESPACE)
                .getOrComputeIfAbsent(CONTEXT_RESOURCE_KEY, key -> createResource(), ContextResource.class);
        CpiTestDsl.bind(resource.context());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Residual verification runs once when the root store is closed.
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return parameterType == CpiTestContext.class || parameterType == CpiTestConfig.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        ContextResource resource = extensionContext.getRoot().getStore(NAMESPACE)
                .get(CONTEXT_RESOURCE_KEY, ContextResource.class);
        if (resource == null) {
            throw new ParameterResolutionException("CpiTestContext has not been initialized");
        }
        Class<?> parameterType = parameterContext.getParameter().getType();
        if (parameterType == CpiTestContext.class) {
            return resource.context();
        }
        if (parameterType == CpiTestConfig.class) {
            return resource.context().getConfig();
        }
        throw new ParameterResolutionException("Unsupported parameter type: " + parameterType.getName());
    }

    private ContextResource createResource() {
        CpiTestConfig config = new ConfigLoader().load();
        OkHttpTransport transport = new OkHttpTransport();
        CpiTestContext context = new CpiTestContext(
                RunIdGenerator.generate(),
                config,
                new Ledger(),
                new CpiMonitoringClient(transport, config),
                new CollectorClient(transport, config.getCollectorBaseUrl()),
                new ResidualChecker());
        return new ContextResource(context);
    }

    private static final class ContextResource implements ExtensionContext.Store.CloseableResource {
        private final CpiTestContext context;

        private ContextResource(CpiTestContext context) {
            this.context = context;
        }

        private CpiTestContext context() {
            return context;
        }

        @Override
        public void close() {
            List<ResidualDocument> residuals = context.getResidualChecker()
                    .checkResiduals(context.getLedger(), context.getCollectedDocuments());
            CpiTestDsl.clear();
            if (!residuals.isEmpty()) {
                String details = residuals.stream()
                        .map(residual -> "messageId=" + residual.messageId()
                                + ", correlationId=" + residual.correlationId()
                                + ", assignedTestCase=" + residual.assignedTestCase())
                        .collect(Collectors.joining("; "));
                throw new AssertionError("Residual documents detected for runId=" + context.getRunId() + ": " + details);
            }
        }
    }
}
