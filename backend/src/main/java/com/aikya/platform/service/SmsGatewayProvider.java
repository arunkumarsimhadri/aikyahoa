package com.aikya.platform.service;

public interface SmsGatewayProvider {

    void sendOtpSms(String phoneNumber, String otpCode, String locale);

    void sendSms(String phoneNumber, String message);
}
