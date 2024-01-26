!supervise.

+!supervise : client(ClientName) & message_box(MessageBoxName) <-
    .my_name(Me);
    println("[", Me, "] I'm starting my day");
    lookupArtifact(ClientName, ClientId);
    searchCupMoverWorkers(WorkerBodies) [artifact_id(ClientId)];
    +cups_moved(0);
    for (.map.key(WorkerBodies, WorkerName) & .map.get(WorkerBodies, WorkerName, WorkerBody)) {
        +body(WorkerName, WorkerBody);
        focusWorkerBody(WorkerBody) [artifact_id(ClientId)];
        println("[", Me, "] I've seen ", WorkerName, " at work");
        assignMoveCupTask(WorkerBody, "shop_floor", "warehouse") [artifact_id(ClientId)];
        println("[", Me, "] I've told ", WorkerName, " to move one cup");
    };
    lookupArtifact(MessageBoxName, MessageBoxId);
    resolveNextSignal [artifact_id(MessageBoxId)];
    resolveNextSignal [artifact_id(MessageBoxId)];
    resolveNextSignal [artifact_id(MessageBoxId)];
    resolveNextSignal [artifact_id(MessageBoxId)];

+move("lenny") : cups_moved(N) <-
    .my_name(Me);
    println("[", Me, "] Lenny did a good job!");
    -+cups_moved(N + 1).

+move("homer") : client(ClientName) & message_box(MessageBoxName) & body("homer", HomerBody) & body("lenny", LennyBody) <-
    .my_name(Me);
    println("[", Me, "] Homer did a bad job, I need to tell him!");
    lookupArtifact(ClientName, ClientId);
    punish(HomerBody) [artifact_id(ClientId)];
    assignMoveCupTask(LennyBody, "shop_floor", "warehouse") [artifact_id(ClientId)];
    lookupArtifact(MessageBoxName, MessageBoxId);
    resolveNextSignal [artifact_id(MessageBoxId)];
    resolveNextSignal [artifact_id(MessageBoxId)].

cups_moved(2) : true <-
    .my_name(Me);
    println("[", Me, "] My job here is done!");

{ include("$jacamoJar/templates/common-cartago.asl") }