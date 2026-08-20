package me.cxdev.sapbtp.testing.dsl;

import me.cxdev.sapbtp.testing.extension.CpiTestContext;

public final class CpiTestRequest {
    private final CpiTestDsl.RequestStage stage;

    public CpiTestRequest(CpiTestContext context) {
        this.stage = CpiTestDsl.given(context);
    }

    public CpiTestRequest correlationId(String correlationId) {
        stage.correlationId(correlationId);
        return this;
    }

    public CpiTestRequest messageId(String messageId) {
        stage.messageId(messageId);
        return this;
    }

    public CpiTestRequest expectDocuments(int expectedDocuments) {
        stage.expectDocuments(expectedDocuments);
        return this;
    }

    public CpiTestDsl.WhenStage when() {
        return stage.when();
    }
}
