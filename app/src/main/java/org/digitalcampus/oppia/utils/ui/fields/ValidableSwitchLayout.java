package org.digitalcampus.oppia.utils.ui.fields;

import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.digitalcampus.mobile.learning.R;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

public class ValidableSwitchLayout extends LinearLayout implements ValidableField{

    private boolean required = false;
    private SwitchCompat input;
    private TextView helperText;
    private TextView errorText;

    public ValidableSwitchLayout(Context context){
        super(context);
    }

    public ValidableSwitchLayout(Context context, SwitchCompat input) {
        super(context);
        this.setOrientation(VERTICAL);

        this.addView(input);
        this.input = input;

        errorText = new TextView(getContext());
        errorText.setLayoutParams(CustomFieldsUIManager.getLinearParams());
        errorText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        errorText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_error));
        errorText.setText(getResources().getString(R.string.field_required));
        errorText.setVisibility(GONE);
        addView(errorText);
    }

    @Override
    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setChecked(boolean checked){
        input.setChecked(checked);
    }

    public boolean isChecked(){
        return input.isChecked();
    }

    @Override
    public void initialize() {
        if (required && input != null){
            this.input.setHint(input.getHint() + " *");
        }

    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params){
        int margin = getContext().getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
        LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) params;
        linearParams.setMargins(0, margin, 0, margin);
        super.setLayoutParams(linearParams);
    }

    @Override
    public boolean validate() {
        boolean valid = !required || input.isChecked();
        errorText.setVisibility(valid ? GONE : VISIBLE);
        input.setHintTextColor(ContextCompat.getColor(getContext(), valid ? android.R.color.tab_indicator_text : R.color.text_error));
        return valid;
    }

    @Override
    public void setHelperText(CharSequence text) {
        if (helperText == null) {
            helperText = new TextView(getContext());
            helperText.setLayoutParams(CustomFieldsUIManager.getLinearParams());
            helperText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            this.addView(helperText);
        }
        helperText.setText(text);

    }

    @Override
    public String getCleanedValue() {
        return input.isChecked() ? "true" : "false";
    }


    @Override
    public void setChangeListener(final onChangeListener listener) {
        input.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                listener.onValueChanged(input.isChecked() ? "true" : null);
            }
        });
    }
}
