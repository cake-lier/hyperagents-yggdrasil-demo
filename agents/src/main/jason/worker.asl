!work.

+!work : client(ClientName) & message_box(MessageBoxName) <-
    .print("I'm starting my day");
    lookupArtifact(ClientName, ClientId);
    joinProductionWorkspace(AgentBody) [artifact_id(ClientId)];
    .print("I entered the shop floor");
    searchForRoboticArm(AgentBody, RobotArm) [artifact_id(ClientId)];
    +robot_arm(RobotArm);
    .print("I found my robotic arm");
    lookupArtifact(MessageBoxName, MessageBoxId);
    !loop(MessageBoxId).

+!loop(MessageBoxId) : true <-
    resolveNextSignal [artifact_id(MessageBoxId)];
    !loop(MessageBoxId).

+move_cup(From, To) : client(ClientName) & well_behaved(WellBehaved) & robot_arm(RobotArm) <-
    lookupArtifact(ClientName, ClientId);
    .print("Starting to use my robotic arm.");
    if (WellBehaved) {
        useRoboticArm(RobotArm, To) [artifact_id(ClientId)];
    } else {
        useRoboticArm(RobotArm, From) [artifact_id(ClientId)];
    };
    .print("Ended using my robotic arm.").

+scold : true <- .print("Oh no!").

{ include("$jacamoJar/templates/common-cartago.asl") }