package com.mcdxai.meteortestharness.dom;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;

import java.util.ArrayList;
import java.util.List;

public final class DomEntryListHelper {
    boolean isEntryElement(Screen screen, Element element) {
        return element instanceof AlwaysSelectedEntryListWidget.Entry<?>;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    boolean isSelectedInList(EntryListWidget<?> entryList, Element element) {
        EntryListWidget rawList = (EntryListWidget) entryList;
        return rawList.getSelectedOrNull() == element;
    }

    boolean isEntrySelected(Screen screen, Element entry) {
        EntryListWidget<?> owner = findOwningEntryList(screen, entry);
        return owner != null && isSelectedInList(owner, entry);
    }

    boolean trySelectEntryAtCoordinates(Screen screen, Click click, boolean doubled) {
        List<EntryListWidget<?>> entryLists = new ArrayList<>();
        for (Element child : screen.children()) {
            collectEntryLists(child, entryLists);
        }

        for (EntryListWidget<?> entryList : entryLists) {
            Element hovered = entryList.hoveredElement(click.x(), click.y()).orElse(null);
            if (hovered instanceof AlwaysSelectedEntryListWidget.Entry<?> && selectEntry(screen, entryList, hovered, click, doubled)) {
                return true;
            }
        }

        return false;
    }

    boolean trySelectEntryFallback(Screen screen, Element entry, Click click, boolean doubled) {
        EntryListWidget<?> owner = findOwningEntryList(screen, entry);
        if (owner == null || !owner.children().contains(entry)) {
            return false;
        }

        return selectEntry(screen, owner, entry, click, doubled);
    }

    EntryListWidget<?> findOwningEntryList(Screen screen, Element entry) {
        for (Element child : screen.children()) {
            EntryListWidget<?> found = findOwningEntryList(child, entry);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private void collectEntryLists(Element element, List<EntryListWidget<?>> entryLists) {
        if (element instanceof EntryListWidget<?> entryList) {
            entryLists.add(entryList);
        }

        if (element instanceof ParentElement parent) {
            for (Element child : parent.children()) {
                if (child == element) continue;
                collectEntryLists(child, entryLists);
            }
        }
    }

    private EntryListWidget<?> findOwningEntryList(Element element, Element entry) {
        if (element instanceof EntryListWidget<?> entryList && entryList.children().contains(entry)) {
            return entryList;
        }

        if (element instanceof ParentElement parent) {
            for (Element child : parent.children()) {
                if (child == element) continue;
                EntryListWidget<?> found = findOwningEntryList(child, entry);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private boolean selectEntry(Screen screen, EntryListWidget<?> entryList, Element entry, Click click, boolean doubled) {
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
