package io.mcdxai.harness.universal.adapter.vanilla;

import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

/** Helps select entries inside vanilla AbstractSelectionList widgets (server list, world list, etc.). */
public final class VanillaEntryListHelper {

    public boolean isEntryElement(Screen screen, GuiEventListener element) {
        return findOwningEntryList(screen, element) != null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean isSelectedInList(AbstractSelectionList<?> entryList, GuiEventListener element) {
        AbstractSelectionList rawList = (AbstractSelectionList) entryList;
        try {
            return rawList.getSelected() == element;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean isEntrySelected(Screen screen, GuiEventListener entry) {
        AbstractSelectionList<?> owner = findOwningEntryList(screen, entry);
        return owner != null && isSelectedInList(owner, entry);
    }

    public boolean trySelectEntryAtCoordinates(Screen screen, MouseButtonEvent click, boolean doubled) {
        List<AbstractSelectionList<?>> entryLists = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            collectEntryLists(child, entryLists);
        }

        for (AbstractSelectionList<?> entryList : entryLists) {
            GuiEventListener hovered = entryList.getChildAt(click.x(), click.y()).orElse(null);
            if (hovered != null && selectEntry(screen, entryList, hovered, click, doubled)) {
                return true;
            }
        }

        return false;
    }

    public boolean trySelectEntryFallback(Screen screen, GuiEventListener entry, MouseButtonEvent click, boolean doubled) {
        AbstractSelectionList<?> owner = findOwningEntryList(screen, entry);
        if (owner == null || !owner.children().contains(entry)) {
            return false;
        }
        return selectEntry(screen, owner, entry, click, doubled);
    }

    public AbstractSelectionList<?> findOwningEntryList(Screen screen, GuiEventListener entry) {
        for (GuiEventListener child : screen.children()) {
            AbstractSelectionList<?> found = findOwningEntryList(child, entry);
            if (found != null) return found;
        }
        return null;
    }

    private void collectEntryLists(GuiEventListener element, List<AbstractSelectionList<?>> entryLists) {
        if (element instanceof AbstractSelectionList<?> entryList) entryLists.add(entryList);

        if (element instanceof ContainerEventHandler parent) {
            for (GuiEventListener child : parent.children()) {
                if (child == element) continue;
                collectEntryLists(child, entryLists);
            }
        }
    }

    private AbstractSelectionList<?> findOwningEntryList(GuiEventListener element, GuiEventListener entry) {
        if (element instanceof AbstractSelectionList<?> entryList && entryList.children().contains(entry)) {
            return entryList;
        }
        if (element instanceof ContainerEventHandler parent) {
            for (GuiEventListener child : parent.children()) {
                if (child == element) continue;
                AbstractSelectionList<?> found = findOwningEntryList(child, entry);
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean selectEntry(Screen screen, AbstractSelectionList<?> entryList, GuiEventListener entry, MouseButtonEvent click, boolean doubled) {
        try {
            if (!entryList.children().contains(entry)) return false;

            screen.setFocused(entryList);
            entryList.setFocused(entry);

            if (click != null) {
                entry.mouseClicked(click, doubled);
                entry.mouseReleased(click);
            }

            return isSelectedInList(entryList, entry);
        } catch (Exception ignored) {
            return false;
        }
    }
}
