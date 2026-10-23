package net.aros.haru.value;

import net.aros.haru.runtime.AbstractDialoguePlayer;

public interface HaruFunction {
    Value call(AbstractDialoguePlayer player, Value[] args);
}
