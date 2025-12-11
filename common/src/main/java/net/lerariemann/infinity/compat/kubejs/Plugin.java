package net.lerariemann.infinity.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import net.lerariemann.infinity.block.entity.InfinityPortalBlockEntity;
import net.lerariemann.infinity.compat.kubejs.events.InfinityDimAddedJS;
import net.lerariemann.infinity.compat.kubejs.events.ItemInPortalJS;
import net.lerariemann.infinity.util.teleport.PortalCreator;

public class Plugin extends KubeJSPlugin {

    public static EventGroup INF_EVENTS_GROUP = EventGroup.of("InfinityEvents");

    public static EventHandler ITEM_IN_PORTAL_EVENT = INF_EVENTS_GROUP
            .server("itemInPortal", () -> ItemInPortalJS.class);
    public static EventHandler DIM_ADDED_EVENT = INF_EVENTS_GROUP
            .server("infinityDimAdded", () -> InfinityDimAddedJS.class);

    @Override
    public void registerEvents() {
        INF_EVENTS_GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("InfinityPortalCreator", PortalCreator.class);
        event.add("InfinityPortalBlockEntity", InfinityPortalBlockEntity.class);
    }
}
