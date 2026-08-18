rootProject.name = "sapbtpintegrationtests"

include("task-02-framework", "task-03-suite-template", "task-04-e2e-vertrag")
project(":task-02-framework").projectDir = file("tasks/task-02-framework")
project(":task-03-suite-template").projectDir = file("tasks/task-03-suite-template")
project(":task-04-e2e-vertrag").projectDir = file("tasks/task-04-e2e-vertrag")
