
package storeaui.preference;

import android.content.Context;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.Switch;
import java.lang.reflect.Field;

import com.hw.droid.R;

public class HWSwitchPreference extends SwitchPreference {

    private static final String TAG ="HWSwitchPreference";

    public HWSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFlag();
    }

    public HWSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFlag();
    }

    public HWSwitchPreference(Context context) {
        super(context);
        setFlag();
    }

    private final Listener mListener = new Listener();

    private class Listener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (!callChangeListener(isChecked)) {
                buttonView.setChecked(!isChecked);
                return;
            }

            HWSwitchPreference.this.setChecked(isChecked);
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        final View layout = layoutInflater.inflate(R.layout.hw_common_preference, parent, false);

        final ViewGroup widgetFrame = (ViewGroup) layout.findViewById(android.R.id.widget_frame);
        if (widgetFrame != null) {
            layoutInflater.inflate(R.layout.hw_common_preference_widget_switch, widgetFrame);
        }

        return layout;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View checkableView = view.findViewById(R.id.switchWidget);
        if (checkableView != null && checkableView instanceof Checkable) {
            final boolean isswitch = checkableView instanceof Switch;
            if (isswitch) {
                final Switch switchView = (Switch) checkableView;
                switchView.setOnCheckedChangeListener(null);
            }

            ((Checkable) checkableView).setChecked(isChecked());

            if (isswitch) {
                final Switch switchView = (Switch) checkableView;
                switchView.setTextOn("");
                switchView.setTextOff("");
                switchView.setOnCheckedChangeListener(mListener);
            }
        }
    }
    private void setFlag(){
         try {
            Class<?> c = (Class<?>) Class.forName(Preference.class.getName());
            Field flagField = c.getDeclaredField("mCanRecycleLayout");
            flagField.setAccessible(true);
            Object flagObject = flagField.get(this);
            flagField.set(this,true);
         } catch (ClassNotFoundException e) {
            Log.e(TAG,"setFlag Error : "+e);
         } catch(ExceptionInInitializerError e){
            Log.e(TAG,"setFlag Error : "+e);
         } catch(LinkageError e){
            Log.e(TAG,"setFlag Error : "+e);
         } catch(NoSuchFieldException e){
            Log.e(TAG,"setFlag Error : "+e);
         } catch(NullPointerException e){
            Log.e(TAG,"setFlag Error : "+e);
         } catch(IllegalArgumentException e){
            Log.e(TAG,"setFlag Error : "+e);
         } catch(IllegalAccessException e){
            Log.e(TAG,"setFlag Error : "+e);
         }
    }
}
