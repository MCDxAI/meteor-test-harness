package io.mcdxai.harness.dom;

import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

public final class DomEntryListHelper {
    boolean isEntryElement(Screen screen, GuiEventListener element) {
        return findOwningEntryList(screen, element) != null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    boolean isSelectedInList(AbstractSelectionList<?> entryList, GuiEventListener element) {
        AbstractSelectionList rawList = (AbstractSelectionList) entryList;
        return rawList.getSelected() == element;
    }

    boolean isEntrySelected(Screen screen, GuiEventListener entry) {
        AbstractSelectionList<?> owner = findOwningEntryList(screen, entry);
        return owner != null && isSelectedInList(owner, entry);
    }

    boolean trySelectEntryAtCoordinates(Screen screen, MouseButtonEvent click, boolean doubled) {
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

    boolean trySelectEntryFallback(Screen screen, GuiEventListener entry, MouseButtonEvent click, boolean doubled) {
        AbstractSelectionList<?> owner = findOwningEntryList(screen, entry);
        if (owner == null || !owner.children().contains(entry)) {
            return false;
        }

        return selectEntry(screen, owner, entry, click, doubled);
    }

    AbstractSelectionList<?> findOwningEntryList(Screen screen, GuiEventListener entry) {
        for (GuiEventListener child : screen.children()) {
            AbstractSelectionList<?> found = findOwningEntryList(child, entry);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private void collectEntryLists(GuiEventListener element, List<AbstractSelectionList<?>> entryLists) {
        if (element instanceof AbstractSelectionList<?> entryList) {
            entryLists.add(entryList);
        }

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
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private boolean selectEntry(Screen screen, AbstractSelectionList<?> entryList, GuiEventListener entry, MouseButtonEvent click, boolean doubled) {
        try {
            if (!entryList.children().contains(entry)) {
                return false;
            }

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
