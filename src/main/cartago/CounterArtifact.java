import cartago.Artifact;
import cartago.OPERATION;

public class CounterArtifact extends Artifact {

    private void init(final int count) {
        defineObsProperty("count", count);
    }

    @OPERATION
    public void increment() {
        final var observableCount = getObsProperty("count");
        observableCount.updateValue(observableCount.intValue() + 1);
    }
}
