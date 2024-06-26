package com.example.addressbook.utils;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import com.example.addressbook.R;
import com.example.addressbook.bean.People;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;

import org.litepal.LitePal;
public class TXTImporter {

    private static final int[] pictures = {
            R.drawable.p21, R.drawable.p22, R.drawable.p23, R.drawable.p24, R.drawable.p25,
            R.drawable.p6, R.drawable.p7, R.drawable.p8, R.drawable.p9, R.drawable.p10,
    };

    private final Random random = new Random();

    //使用文件输入流在指定文件地址读出数据
    public void importFromTXT(ContentResolver contentResolver, Uri uri) {
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                parseAndSaveContact(line);
            }
            Log.d("FileImport", "Successfully imported contacts from file.");
        } catch (IOException e) {
            Log.e("FileRead", "Error reading file content", e);
        }
    }

    private void parseAndSaveContact(String line) {
        if (line == null || line.trim().isEmpty()) {
            return; // Skip empty lines
        }

        // 解析每行的联系人信息
        String[] parts = line.split(", ");
        if (parts.length < 4) {
            Log.e("FileImport", "Invalid contact format: " + line);
            return; // Invalid format, skip this line
        }

        String name = parts[0].replace("Name: ", "").trim();
        String group = parts[1].replace("Group: ", "").trim();
        String telephone = parts[2].replace("Telephone: ", "").trim();
        String information = parts[3].replace("Information: ", "").trim();

        // 检查联系人是否已存在
        List<People> existingPeople = LitePal.where("telephone = ?", telephone).find(People.class);
        People person;
        if (existingPeople.size() > 0) {
            person = existingPeople.get(0); // 更新现有联系人
        } else {
            person = new People(); // 创建新联系人
            person.setId(System.currentTimeMillis()); // 设置新联系人ID
            person.setPicturePath(getRandomPicture()); // 随机分配头像
        }

        person.setName(name);
        person.setG(group);
        person.setTelephone(telephone);
        person.setInformation(information);

        // 设置拼音信息
        String pinyin = PinYinStringHelper.getPingYin(name); // 全拼
        person.setPinyin(pinyin);
        String word = PinYinStringHelper.getAlpha(name); // 大写首字母或特殊字符
        person.setWord(word);
        String jianpin = PinYinStringHelper.getPinYinHeadChar(name); // 简拼
        person.setJianpin(jianpin);

        // 保存或更新联系人
        person.save();

        Log.d("FileImport", "Saved contact: " + name);
    }

    private int getRandomPicture() {
        return pictures[random.nextInt(pictures.length)];
    }
}