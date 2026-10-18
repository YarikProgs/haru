package net.aros.haru.value;

public interface HaruFunction {
    default Value call(Value[] args) {
        throw new IllegalStateException("Not implemented");
    }

    default Value call(Value arg1, Value arg2, Value arg3) {
        throw new IllegalStateException("Not implemented");
    }

    default Value call(Value arg1, Value arg2) {
        throw new IllegalStateException("Not implemented");
    }

    default Value call(Value arg1) {
        throw new IllegalStateException("Not implemented");
    }

    default Value call() {
        throw new IllegalStateException("Not implemented");
    }

    default Value dispatchCall(Value[] args) {
        return switch (args.length) {
            case 0 -> call();
            case 1 -> call(args[0]);
            case 2 -> call(args[0], args[1]);
            case 3 -> call(args[0], args[1], args[2]);
            default -> call(args);
        };
    }
}
