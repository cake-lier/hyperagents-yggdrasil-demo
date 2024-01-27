!supervise.

+!supervise : client(ClientName) & message_box(MessageBoxName) <-
    .print("I'm starting my day");
    lookupArtifact(ClientName, ClientId);
    searchCupMoverWorkers(WorkerBodies) [artifact_id(ClientId)];
    +cups_moved(0);
    for (.member(map(WorkerName, WorkerBody), WorkerBodies)) {
        +body(WorkerName, WorkerBody);
        focusWorkerBody(WorkerName, WorkerBody) [artifact_id(ClientId)];
        .print("I've seen ", WorkerName, " at work");
        assignMoveCupTask(WorkerBody, "shop_floor", "warehouse") [artifact_id(ClientId)];
        .print("I've told ", WorkerName, " to move one cup");
    };
    lookupArtifact(MessageBoxName, MessageBoxId);
    !loop(MessageBoxId).

+!loop(MessageBoxId) : true <-
    resolveNextSignal [artifact_id(MessageBoxId)];
    !loop(MessageBoxId).

+moveToWarehouse("actionSucceeded", WorkerName) : cups_moved(N) <-
    .print(WorkerName, " did a good job!");
    -+cups_moved(N + 1).

+moveToShopFloor("actionSucceeded", WorkerName) : client(ClientName) & message_box(MessageBoxName) & body(WorkerName, WorkerBody) <-
    .print(WorkerName, " did a bad job, I need to tell him!");
    lookupArtifact(ClientName, ClientId);
    punish(WorkerBody) [artifact_id(ClientId)];
    if (WorkerName == "homer") {
        ?body("lenny", OtherWorkerBody);
    } elif (WorkerName == "lenny") {
        ?body("homer", OtherWorkerBody);
    };
    assignMoveCupTask(OtherWorkerBody, "shop_floor", "warehouse") [artifact_id(ClientId)].

+cups_moved(2) : true <- .print("My job here is done!").

{ include("$jacamoJar/templates/common-cartago.asl") }