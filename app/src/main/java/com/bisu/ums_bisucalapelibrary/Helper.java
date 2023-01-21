package com.bisu.ums_bisucalapelibrary;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Helper {

    private Context context;
    private static final String SHARED_PREF_NAME = "umsbisucalapelibrary";

    public Helper(Context context) {
        this.context = context;
    }

    public void saveUser(String id, String photoUrl, String fullName, String username){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("id", id);
        editor.putString("photoUrl", photoUrl);
        editor.putString("fullName", fullName);
        editor.putString("username", username);
        editor.apply();
    }

    public String getId(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("id", null);
    }

    public String getPhotoUrl(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("photoUrl", null);
    }

    public String getFullName(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("fullName", null);
    }

    public String getUsername(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("username", null);
    }

    public void clearUser(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public Uri getImageUri(Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, String.valueOf(System.currentTimeMillis()), null);
        return Uri.parse(path);
    }

    public String formatDate(String date){
        LocalDate ld = LocalDate.parse(date);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return ld.format(dtf);
    }

    public String getNumberDate(String date){
        LocalDate ld = LocalDate.parse(date);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return ld.format(dtf);
    }

    public String formatDate(Date date){
        return new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a").format(date);
    }

    public String formatDate(long dateInMillis){
        return new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a").format(dateInMillis);
    }

    public String calculateAge(String bdate) {
        LocalDate dob = LocalDate.parse(bdate);
        LocalDate cur_date = LocalDate.now();
        int age = Period.between(dob, cur_date).getYears();
        return String.valueOf(age);
    }

    public String capitalize(String str){
        String res = null;
        String temp = str.replaceAll("\\s+"," ").trim();
        String extractedString[] = temp.split("\\s");

        StringBuilder builder = new StringBuilder();
        for(String s : extractedString){
            char firstChar = s.toUpperCase().charAt(0);
            String restChars = s.substring(1, s.length()).toLowerCase();
            builder.append(firstChar + restChars + " ");
        }
        res = builder.toString().replaceAll("\\s+"," ").trim();
        return res;
    }

    public String getFileExtFromUri(Uri uri){
        ContentResolver cr = context.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(uri));
    }

    public Date getStartDate(LocalDate localDate){
        ZonedDateTime startOfDayInAsia = localDate.atStartOfDay(ZoneId.of("Asia/Singapore"));
        long millis = startOfDayInAsia.toInstant().toEpochMilli();
        Date startDate = new Date();
        startDate.setTime(millis);
        return startDate;
    }

    public Date getEndDate(LocalDate localDate){
        ZonedDateTime endOfDayInAsia = localDate.atTime(LocalTime.MAX).atZone(ZoneId.of("Asia/Singapore"));
        long millis = endOfDayInAsia.toInstant().toEpochMilli();
        Date startDate = new Date();
        startDate.setTime(millis);
        return startDate;
    }

    public long getEndDateInMillis(String date){
        LocalDate localDate = LocalDate.parse(date);
        ZonedDateTime endOfDayInAsia = localDate.atTime(LocalTime.MAX).atZone(ZoneId.of("Asia/Singapore"));
        long longDate = endOfDayInAsia.toInstant().toEpochMilli();
        return longDate;
    }

    public LocalDate findMinDOBOfAge(int age){
        LocalDate today = LocalDate.now();
        LocalDate ld = today.minusYears(age+1);
        ld = ld.plusDays(1);
        return ld;
    }


}
