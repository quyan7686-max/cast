package com.stillwater.fishing.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

import java.util.List;

/**
 * Exposes the canvas-drawn controls as virtual accessibility nodes so TalkBack can focus and
 * activate each one, instead of seeing the whole game as a single unreadable view.
 */
final class FishingAccessibility extends AccessibilityNodeProvider {
    static final int HOST_ID = View.NO_ID;

    static final class Item {
        final int id;
        final String label;
        final RectF bounds;
        final boolean clickable;
        final boolean selected;

        Item(int id, String label, RectF bounds, boolean clickable, boolean selected) {
            this.id = id;
            this.label = label;
            this.bounds = bounds;
            this.clickable = clickable;
            this.selected = selected;
        }
    }

    private final FishingView host;
    private List<Item> cached;
    private int focusedId = HOST_ID;
    private int hoveredId;

    FishingAccessibility(FishingView host) {
        this.host = host;
    }

    @Override
    public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
        if (virtualViewId == HOST_ID) return hostNode();
        Item item = findItem(virtualViewId);
        return item == null ? null : itemNode(item);
    }

    private AccessibilityNodeInfo hostNode() {
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(host);
        host.onInitializeAccessibilityNodeInfo(info);
        return info;
    }

    private AccessibilityNodeInfo itemNode(Item item) {
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(host, item.id);
        info.setPackageName(host.getContext().getPackageName());
        info.setClassName("android.widget.Button");
        info.setText(item.label);
        info.setContentDescription(item.label);
        info.setEnabled(true);
        info.setFocusable(true);
        info.setVisibleToUser(true);
        info.setSelected(item.selected);
        if (item.clickable) {
            info.setClickable(true);
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
        }
        if (focusedId == item.id) info.setAccessibilityFocused(true);
        info.setParent(host);
        info.setBoundsInScreen(host.screenBoundsOf(item.bounds));
        return info;
    }

    @Override
    public boolean performAction(int virtualViewId, int action, Bundle arguments) {
        if (virtualViewId == HOST_ID) return host.performAccessibilityAction(action, arguments);
        Item item = findItem(virtualViewId);
        if (item == null) return false;
        if (action == AccessibilityNodeInfo.ACTION_CLICK) {
            if (!item.clickable) return false;
            host.activate(item.id);
            cached = null;
            sendEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) {
            if (focusedId == virtualViewId) return true;
            if (focusedId != HOST_ID) {
                sendEvent(focusedId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
            }
            focusedId = virtualViewId;
            sendEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
            return true;
        }
        if (action == AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS) {
            if (focusedId != virtualViewId) return true;
            focusedId = HOST_ID;
            sendEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED);
            return true;
        }
        return false;
    }

    /** Drives touch exploration: hovering a control focuses it for TalkBack. */
    boolean dispatchHover(MotionEvent event) {
        int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_HOVER_MOVE
                && action != MotionEvent.ACTION_HOVER_ENTER
                && action != MotionEvent.ACTION_HOVER_EXIT) {
            return false;
        }
        int id = action == MotionEvent.ACTION_HOVER_EXIT ? 0
                : host.itemAt(host.logicalX(event.getX()), host.logicalY(event.getY()));
        if (id == hoveredId) return id != 0;
        if (hoveredId != 0) sendEvent(hoveredId, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
        hoveredId = id;
        if (hoveredId != 0) sendEvent(hoveredId, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
        return id != 0;
    }

    /** The panel or its contents changed, so cached nodes and stale focus must be dropped. */
    void rebuild() {
        cached = null;
        hoveredId = 0;
        if (focusedId != HOST_ID && findItem(focusedId) == null) focusedId = HOST_ID;        AccessibilityEvent event = AccessibilityEvent.obtain(
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        event.setContentChangeTypes(AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE);
        event.setPackageName(host.getContext().getPackageName());
        event.setSource(host, HOST_ID);
        send(event);
    }

    List<Item> items() {
        if (cached == null) cached = host.accessibilityItems();
        return cached;
    }

    private Item findItem(int id) {
        for (Item item : items()) {
            if (item.id == id) return item;
        }
        return null;
    }

    private void sendEvent(int virtualViewId, int type) {
        AccessibilityEvent event = AccessibilityEvent.obtain(type);
        event.setPackageName(host.getContext().getPackageName());
        event.setClassName("android.widget.Button");
        event.setSource(host, virtualViewId);
        send(event);
    }

    private void send(AccessibilityEvent event) {
        ViewParent parent = host.getParent();
        AccessibilityManager manager = (AccessibilityManager) host.getContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        // API 26 throws from requestSendAccessibilityEvent when no service is listening.
        if (parent == null || manager == null || !manager.isEnabled()) {
            event.recycle();
            return;
        }
        parent.requestSendAccessibilityEvent(host, event);
    }
}
