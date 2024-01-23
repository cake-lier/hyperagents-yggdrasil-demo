!hello.

+!hello : true <-
    println("Hello world!");
    makeArtifact("counter", "CounterArtifact", [0], CounterId);
    focus(CounterId);
    increment.

+count(N) : N < 3 <-
    println("The count is now at: ", N, ".");
    increment.

+count(3) : true <-
    println("The count is now at 3, shutting down.").

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }