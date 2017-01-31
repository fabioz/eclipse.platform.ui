# To be added to https://wiki.eclipse.org/E4/Macros when integrated.

Basic information on how macros should work and what is expected of clients.

The basic idea in this implementation is that the macro plugin is a thin layer
used to record macro commands and then play it back. The actual work is done
on the actual plugins, which should be macro-aware.

The macro recording doesn't only map to text editions. Anything done in the
IDE could be recorded in a macro to be played back later on. For instance,
it may be possible to record and playback changes to preferences, importing
some git repository, finding text, etc. Later on, when playing it back, it
should be even possible to ask if the user wants to edit some of the values
which were saved during record.

Still, note that while that support will be possible, currently the macro 
will target only the text editor and its operations.

Abstract structure
================================================

The basic idea is that editors listen or detect when macro record mode is active
and upon entering macro mode, they make sure that everything done will be later 
played back accordingly. 

-- note that it's important that things are not the other way around: the 
macro plugin is a thin layer to start/stop macro recording and provide the basic 
abstractions and management of macros, it's up to the clients 
to be aware that they are in macro record mode and act accordingly, 
issuing commands to be recorded and later played back.

e.g.: some editors may add a closing quote or closing parenthesis 
automatically. While it's Ok for them to make that in record mode, they
have to make sure that a proper action is recorded for this case and later
on when it's played back the same behavior is done. Things such as 
auto-completion should also respect that (or be disabled in record mode).

Actual implementation
======================

Clients can either listen for changes on the macro mode (i.e.: when a record
or playback will start) or just query the current state.

Scenario 1:

Setting up state when entering macro record/playback mode:

```
public class MyMacroAwareClass implements IMacroListener {

    public void init(final IEditorSite site) {
        fMacroContext = site.getService(EMacroContext.class);
        if (fMacroContext != null) {
            fMacroContext.addMacroListener(this);
            if (fMacroContext.isRecording() || fMacroContext.isPlayingBack()) {
                // If it's already on a record or playback session, enter the
                // mode properly.
                this.onMacroStateChanged(fMacroContext);
            }
        }
    }
    
    public final void onMacroStateChanged(EMacroContext macroContext) {
        boolean isInRecordOrPlaybackMode = macroContext.isRecording() || macroContext.isPlayingBack();
        if (isInRecordOrPlaybackMode && !fInternalStateForRecordPlaybackMode) {
            onEnteredMacroRecordOrPlaybackMode(macroContext);
        } else if (!isInRecordOrPlaybackMode && fInternalStateForRecordPlaybackMode) {
            onLeftMacroRecordOrPlaybackMode(macroContext);
        }
    }

}
```

An actual implementation using this strategy is:

org.eclipse.ui.texteditor.AbstractTextEditor


Scenario 2:

Just issuing commands when no setup is needed:

```
public class MyMacroAwareClass implements IMacroListener {

    @Inject
    EMacroContext fMacroContext;
    
    public void onAction() {
        if(fMacroContext.isRecording()){
            fMacroContext.addCommand(new MyMacroCommand());
        }
    }
}
```

An actual implementation using this strategy is:

org.eclipse.e4.ui.bindings.keys.KeyBindingDispatcher

Whenever a command is added, an extension point registering a class to 
recreate it later on must also be registered through the 
org.eclipse.e4.core.macros.macro_command extension point.

