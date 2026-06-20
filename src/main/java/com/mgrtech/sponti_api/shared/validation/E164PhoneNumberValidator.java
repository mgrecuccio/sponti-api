package com.mgrtech.sponti_api.shared.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class E164PhoneNumberValidator implements ConstraintValidator<ValidE164PhoneNumber, String> {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    private static final String E164_REGEX = "\\+[1-9]\\d{1,14}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if(value == null  || value.isBlank()) {
            return true;
        }

        if(!value.matches(E164_REGEX)) {
            return false;
        }

        try {
            var parsed = PHONE_UTIL.parse(value, null);

            return PHONE_UTIL.isValidNumber(parsed)
                    && value.equals(PHONE_UTIL.format(
                            parsed,
                            PhoneNumberUtil.PhoneNumberFormat.E164
                    ));
        } catch (NumberParseException e) {
            return false;
        }
    }
}
