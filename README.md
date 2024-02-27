# Yggdrasil's latest features demo

This repository demonstrates the features shipped with the latest version of Yggdrasil, part of the "HyperAgents" project.

## Prerequisites

* Java 21+
* Gradle 8.4+
* Docker 24.0.7+

## How do I run it?

1. Position yourself in the project folder, for example, using the ```cd``` command.
2. Build the Yggdrasil platform JAR file using the ```./gradlew environment:shadowJar``` command.
3. Launch the JAR file using the ```docker compose up``` command. 
⚠️ **Since the platform container uses the "host" network, please make sure port 8080 is available for Yggdrasil. 
If not, update the port to open in Yggdrasil's configuration file
```config.json``` in the ```environment/src/main/resources/conf``` folder and also in the "workers.jcm" 
and "management.jcm" configuration files in the ```agents``` folder before building the JAR.** ⚠️
4. Wait until the following message appears onscreen.

```
environment | [vert.x-eventloop-thread-0] INFO io.vertx.core.impl.launcher.commands.VertxIsolatedDeployer - Succeeded in deploying verticle
```

5. Since the environment platform is now running, the JaCaMo platform containing the worker agents needs to be started. 
Use the command ```./gradlew agents:runWorkers``` in a different shell from the one you used for the Docker command. 
⚠️ **Since the agents exchange messages, two more ports are opened on the host machine: 8082 and 8083.
Please make sure both are available to them. 
If not, update the ports to open in the workers' agent system configuration file ```workers.jcm```  in the 
```agents``` folder before launching the command.** ⚠️
6. Wait until the following messages appear in the newly opened application window.

```
[alice] I found my robotic arm
[bob] I found my robotic arm
```

7. Since the workers' agent system is now running, the JaCaMo platform containing the supervisor agent needs to be started.
Use the command ```./gradlew agents:runManagement``` in a different shell from the previous two. 
⚠️ **Since the agent exchanges messages, one more port is opened on the host machine: 8081. 
Please make sure it is available to it. 
If not, update the port to open in the manager's agent system configuration file ```management.jcm```  in the 
```agents``` folder before launching the command.** ⚠️
8. Wait until the following message appears in the newly opened application window.

```
[carl] My job here is done!
```

9. The system has now finished running, so we can examine its logs.

## Clean-up

To close the two windows showing the logs of the JaCaMo platforms, press their "stop" button.
To terminate the Yggdrasil platform, press CTRL+C in the shell which is executing it, and then,
after the container stops, run the command ```docker compose down``` to delete the container. 
If you want a complete clean-up, delete the Yggdrasil image by running the command ```docker image rm environment```.

## What is happening?

Each agent receives from the system two artifacts: a "client" and a "message box." 
The first is to make requests to the Yggdrasil platform, 
while the second is to receive notifications back using the "WebSub" protocol.
Following the CArtAgO rules for agents and artifacts communication,
the agents focus on their message box to receive the messages from the environment platform as signals to which, in turn, react. 
It is why we see as first log messages for each agent the following lines.

```
[bob] join workspace /main/workers: done
[bob] focusing on artifact message_box_bob (at workspace /main/workers) using namespace default
[bob] focus on message_box_bob: done

[alice] join workspace /main/workers: done
[alice] focusing on artifact message_box_alice (at workspace /main/workers) using namespace default
[alice] focus on message_box_alice: done

[carl] join workspace /main/managers: done
[carl] focusing on artifact message_box_carl (at workspace /main/managers) using namespace default
[carl] focus on message_box_carl: done
```

The creation and focusing of artifacts is part of the configuration specified through the JCM files in the project. 
Then, the Bob and Alice agents, which in this use case represent the workers, start their day. 
It begins by joining the production workspace representing their place of work in the production plant. 
It allows them to acquire a body representation in the "production" workspace,
an operation that ends successfully after the following lines are printed onscreen.

```
[alice] I'm starting my day
[bob] I'm starting my day
[alice] I entered the shop floor
[bob] I entered the shop floor
```

Then, the two agents "look around" their workspace by querying the workspace representation on the environment platform using a SPARQL query. 
If they find the robotic arm assigned to them by the company,
they will add the representation of an action affordance for sending a message to use it to their body. 
In addition, they always add an affordance to communicate a generic message to their body representation to allow sending them some information, 
something that will come in handy later. 
The operation concludes with sending the new body representation to the Yggdrasil platform. 
The operation is successful when the following two log lines are printed.

```
[alice] I found my robotic arm
[bob] I found my robotic arm
```

Having found their robotic arm, the two agents wait for new messages from their supervisor regarding tasks involving it.
At this point, the supervisor agent boots up and starts its day with the following message.

```
[carl] I'm starting my day
```

It knows he needs two workers to put two cups in the warehouse from the shop floor.
It then searches for these two workers using a SPARQL query.
The search for workers happens by looking for two body artifacts that offer to send them a message to accomplish a "move cup" goal.
Carl, the supervisor, indeed finds the two workers.
For each of them, it focuses on their body to check on their actions using the "WebSub" protocol offered by the environment platform.
It happens when each of the following lines gets printed onscreen.

```
[carl] I've seen alice at work
[carl] I've seen bob at work
```

After the focusing action, Carl tasks the two agents to move a cup from the shop floor to the warehouse.
It uses the information the agent knows about the workers found in their Thing Description,
namely the "method name" and the "target URI" of the operation, and sends an appropriate payload.
It happens when the following gets logged onto the screen.

```
[carl] I've told alice to move one cup
[carl] I've told bob to move one cup
```

The two agents receive the goals to accomplish as signals from their message boxes.
The agents receive the message and begin to do their job when the following two messages appear.

```
[alice] Starting to use my robotic arm.
[bob] Starting to use my robotic arm.
```

If the agent is "well-behaved," it will use its robotic arm correctly, i.e., doing the right action on it.
Otherwise, it will use the wrong action.
The Thing Description of their robotic arm, found at the beginning of their day, 
contains the information for doing the action they want to perform. 
Namely, they use the "method name" and the "target URI" in the action affordance form. 
The robotic arms receive their message and then start moving when the following two messages get printed on the standard output of the Yggdrasil platform.

```
[robot_arm1] Moved arm to warehouse
[robot_arm2] Moved arm to shop floor
```

The ending of the operations is signaled agent-side by the following two log lines.

```
[bob] Ended using my robotic arm.
[alice] Ended using my robotic arm.
```

Since the supervisor was looking for the actions done by the workers, it will receive their start and end signals through its message box.
If the action for moving a cup to the warehouse is received, then the number of cups in storage is incremented by one.
Carl thinks to itself about the excellent job done by the agent with the following message.

```
[carl] alice did a good job!
```

If, instead, an action for moving a cup from the warehouse to the shop floor happens, disaster ensues, 
and the supervisor agent thinks to itself the following thought.

```
[carl] bob did a bad job, I need to tell him!
```

Then, it punishes the agent for doing such a poor job by sending a message using again its body Thing Description.
In particular, it uses the information about the action affordance to send a generic message as it did before. 
The Bob agent receives the punishment by exclaiming the following message onscreen.

```
[bob] Oh no!
```

Then, it chooses the other agent, who did not misbehave, and asks him to move a cup again from the shop floor to the warehouse. 
The agent complies by printing the following log lines.

```
[alice] Starting to use my robotic arm.
[alice] Ended using my robotic arm.
```

We see on the standard output of the environment platform the same message as before regarding the use of the robotic arm.

```
[robot_arm1] Moved arm to warehouse
```

After thinking again about the proper job the well-behaved agent did,
Carl recognizes that two cups are finally in storage and that nothing is left to do.
So, these last two lines are printed onscreen, and the system ends.

```
[carl] alice did a good job!
[carl] My job here is done!
```

## Final remarks

The system demonstrates a highly dynamic behavior.
The worker agents only start with having the network location of the environment platform and the workspace they need to join hard-coded.
Everything else is up to them to find.
They do not know anything about the robotic arms except the workspace to which they belong, their semantic type, and the fact they belong to them.
The agents' bodies change in response to the context they find themselves in.
At the same time, how to use the robotic arm is not hard-coded, only the name of the operation to perform, which depends on their "well-behavedness."
Also, whether the agent decides to well-behave is not part of the agent's plan but can be changed using an appropriate variable.
If both agents behave well, no punishment gets sent.
If both agents misbehave, punishments and task requests get continually sent in a loop.
The same goes for the supervisor agent: only the network location of the Yggdrasil platform is hard-coded.
It does not know from the beginning if and how many worker agents can get the task.
At the same time, it is not hard-coded how to communicate with them once found. 
