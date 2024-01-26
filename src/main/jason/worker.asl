!work.

+!work : client(ClientName) & message_box(MessageBoxName) <-
    .my_name(Me);
    println("[", Me, "] I'm starting my day");
    lookupArtifact(ClientName, ClientId);
    joinProductionWorkspace(AgentBody) [artifact_id(ClientId)];
    println("[", Me, "] I entered the shop floor");
    searchForRoboticArm(AgentBody, RobotArm) [artifact_id(ClientId)];
    +robot_arm(RobotArm);
    println("[", Me, "] I found my robotic arm");
    lookupArtifact(MessageBoxName, MessageBoxId);
    !loop(MessageBoxId).

+!loop(MessageBoxId) : true <-
    resolveNextSignal [artifact_id(MessageBoxId)];
    !loop.

+move_cup(Me, From, To) : .my_name(Me) & client(ClientName) & well_behaved(WellBehaved) & robot_arm(RobotArm) <-
    lookupArtifact(ClientName, ClientId);
    println("[", Me, "] Starting to use my robotic arm.");
    if (WellBehaved) {
        useRoboticArm(RobotArm, From, To) [artifact_id(ClientId)];
    } else {
        useRoboticArm(RobotArm, To, From) [artifact_id(ClientId)];
    };
    println("[", Me, "] Ended using my robotic arm.").

+scold : true <-
    .my_name(Me);
    println("[", Me, "] D'oh!");

{ include("$jacamoJar/templates/common-cartago.asl") }