package com.mgrtech.sponti_api.auth.api;

public record LoginCommand(String phoneNumber, String password) {
}
