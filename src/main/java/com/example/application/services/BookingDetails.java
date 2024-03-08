package com.example.application.services;

import com.example.application.data.BookingStatus;

import java.time.LocalDate;

public record BookingDetails(String bookingNumber,
                             String firstName,
                             String lastName,
                             LocalDate date,
                             BookingStatus bookingStatus,
                             String from,
                             String to,
                             String bookingClass) {
    @Override
    public String toString() {
        return "BookingDetails{" +
                "bookingNumber='" + bookingNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", date=" + date +
                ", bookingStatus=" + bookingStatus +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", bookingClass='" + bookingClass + '\'' +
                '}';
    }
}
