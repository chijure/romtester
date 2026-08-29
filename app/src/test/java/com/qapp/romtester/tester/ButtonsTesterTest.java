package com.qapp.romtester.tester;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;

import org.junit.Test;

public class ButtonsTesterTest {

    private SharedPreferences.Editor mockEditor(SharedPreferences prefs) {
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(prefs.edit()).thenReturn(editor);
        when(editor.remove(anyString())).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);
        return editor;
    }

    @Test
    public void isTested_falseForAllTrackedKeysInitially() {
        Context context = mock(Context.class);
        ButtonsTester tester = new ButtonsTester(context, mock(ButtonsTester.Listener.class));

        for (int keyCode : ButtonsTester.TRACKED_KEYS) {
            assertFalse(tester.isTested(keyCode));
        }
    }

    @Test
    public void handleKeyEvent_marksTrackedKeyTestedOnDown() {
        Context context = mock(Context.class);
        ButtonsTester.Listener listener = mock(ButtonsTester.Listener.class);
        ButtonsTester tester = new ButtonsTester(context, listener);

        tester.handleKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, true);

        assertTrue(tester.isTested(KeyEvent.KEYCODE_VOLUME_UP));
        verify(listener).onKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, true);
    }

    @Test
    public void handleKeyEvent_upEventDoesNotMarkTested() {
        Context context = mock(Context.class);
        ButtonsTester.Listener listener = mock(ButtonsTester.Listener.class);
        ButtonsTester tester = new ButtonsTester(context, listener);

        tester.handleKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, false);

        assertFalse(tester.isTested(KeyEvent.KEYCODE_VOLUME_UP));
        verify(listener).onKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, false);
    }

    @Test
    public void handleKeyEvent_untrackedKeyStillNotifiesListener() {
        Context context = mock(Context.class);
        ButtonsTester.Listener listener = mock(ButtonsTester.Listener.class);
        ButtonsTester tester = new ButtonsTester(context, listener);
        int untracked = KeyEvent.KEYCODE_HOME + 500;

        tester.handleKeyEvent(untracked, true);

        assertFalse(tester.isTested(untracked));
        verify(listener).onKeyEvent(untracked, true);
    }

    @Test
    public void checkHomePressedFlag_marksHomeTestedAndClearsPrefWhenFlagSet() {
        Context context = mock(Context.class);
        SharedPreferences prefs = mock(SharedPreferences.class);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs);
        when(prefs.getBoolean(anyString(), anyBoolean())).thenReturn(true);
        SharedPreferences.Editor editor = mockEditor(prefs);
        ButtonsTester.Listener listener = mock(ButtonsTester.Listener.class);
        ButtonsTester tester = new ButtonsTester(context, listener);

        tester.checkHomePressedFlag();

        assertTrue(tester.isTested(ButtonsTester.KEY_HOME_SENTINEL));
        verify(listener).onHomeDetectedFromPreviousSession();
        verify(editor).remove(anyString());
        verify(editor).apply();
    }

    @Test
    public void checkHomePressedFlag_doesNothingWhenFlagNotSet() {
        Context context = mock(Context.class);
        SharedPreferences prefs = mock(SharedPreferences.class);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs);
        when(prefs.getBoolean(anyString(), anyBoolean())).thenReturn(false);
        ButtonsTester.Listener listener = mock(ButtonsTester.Listener.class);
        ButtonsTester tester = new ButtonsTester(context, listener);

        tester.checkHomePressedFlag();

        assertFalse(tester.isTested(ButtonsTester.KEY_HOME_SENTINEL));
        verify(listener, never()).onHomeDetectedFromPreviousSession();
    }

    @Test
    public void markHomeLeavingHint_persistsFlagAsTrue() {
        Context context = mock(Context.class);
        SharedPreferences prefs = mock(SharedPreferences.class);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs);
        SharedPreferences.Editor editor = mockEditor(prefs);
        ButtonsTester tester = new ButtonsTester(context, mock(ButtonsTester.Listener.class));

        tester.markHomeLeavingHint();

        verify(editor).putBoolean(anyString(), org.mockito.ArgumentMatchers.eq(true));
        verify(editor).apply();
    }
}
