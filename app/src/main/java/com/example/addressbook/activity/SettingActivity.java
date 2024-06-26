package com.example.addressbook.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.addressbook.R;
import com.example.addressbook.adapter.SettingGroupAdapter;
import com.example.addressbook.bean.People;
import com.example.addressbook.bean.PeopleGroup;
import com.example.addressbook.utils.DarkModeUtils;
import com.example.addressbook.utils.SPUtils;
import com.example.addressbook.utils.TXTExporter;
import com.example.addressbook.utils.TXTImporter;

import org.litepal.LitePal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class SettingActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int REQUEST_FILE_PICK = 113;

    RadioGroup rg_sort, rg_mode;
    ListView rv;
    SettingGroupAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_activity);
        setTitle("系统设置");

        // 获取并配置 ActionBar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // 配置模式选择 RadioGroup
        rg_mode = findViewById(R.id.rg_mode); // 绑定模式选择的 RadioGroup
        int mode = SPUtils.getInt("mode", 0, SettingActivity.this); // 获取当前模式
        rg_mode.check(mode == 0 ? R.id.mode_day : R.id.mode_night); // 设置初始选中项

        // 设置模式选择的监听器
        rg_mode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.mode_night) {
                    SPUtils.putInt("mode", 1, SettingActivity.this); // 保存夜间模式
                    DarkModeUtils.applyNightMode(SettingActivity.this); // 应用夜间模式
                } else if (checkedId == R.id.mode_day) {
                    SPUtils.putInt("mode", 0, SettingActivity.this); // 保存日间模式
                    DarkModeUtils.applyDayMode(SettingActivity.this); // 应用日间模式
                }
            }
        });

        // 配置排序选择 RadioGroup
        rg_sort = findViewById(R.id.rg_sort);
        int sort = SPUtils.getInt("sort", 0, SettingActivity.this); // 获取当前排序方式
        rg_sort.check(sort == 0 ? R.id.sort_list : R.id.sort_card); // 设置初始选中项

        // 设置排序选择的监听器
        rg_sort.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.sort_card) {
                    SPUtils.putInt("sort", 1, SettingActivity.this); // 保存卡片排序方式
                } else if (checkedId == R.id.sort_list) {
                    SPUtils.putInt("sort", 0, SettingActivity.this); // 保存列表排序方式
                }
            }
        });

        // 配置 ListView 和适配器
        adapter = new SettingGroupAdapter(this, LitePal.findAll(PeopleGroup.class));
        rv = findViewById(R.id.rv);
        rv.setAdapter(adapter);

        // 配置导出按钮的点击监听器
        Button exportButton = findViewById(R.id.btn_export_contacts);
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStoragePermissionAndExport();
            }
        });

        // 配置导入按钮的点击监听器
        Button importButton = findViewById(R.id.btn_import_contacts);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkStoragePermissionAndOpenFilePicker();
            }
        });
    }

    // 检查存储权限并执行导出操作
    private void checkStoragePermissionAndExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // 权限已经授予，继续导出操作
                Log.d("Permission", "Storage management permission already granted");
                exportContacts();
            } else {
                // 请求管理存储权限
                Log.d("Permission", "Requesting storage management permission");
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_WRITE_STORAGE);
            }
        }
    }

    // 检查存储权限并打开文件选择器
    private void checkStoragePermissionAndOpenFilePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // 权限已经授予，继续操作
                Log.d("Permission", "Storage management permission already granted");
                openFilePicker();
            } else {
                // 请求管理存储权限
                Log.d("Permission", "Requesting storage management permission");
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_WRITE_STORAGE);
            }
        }
    }

    // 导出联系人信息
    private void exportContacts() {
        //查询
        List<People> peopleList = LitePal.findAll(People.class);
        //设置将要创建的文件信息
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "contacts.txt");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        //创建文件
        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
        Log.d("Uri is",uri+"");
        //数据写入
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            TXTExporter exporter = new TXTExporter();
            exporter.exportToTXT(peopleList, out);
            Log.d("Export", "Export successful");
        } catch (IOException e) {
            Log.e("Export", "Error exporting contacts", e);
        }
    }

    // 打开文件选择器
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a File"), REQUEST_FILE_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限授予，继续操作
                Log.d("Permission", "Permission granted");
                openFilePicker();
            } else {
                Log.d("Permission", "Permission denied");
                // 权限被拒绝，显示提示信息
                Toast.makeText(this, "Storage permission is necessary to export contacts!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //在用户选择是否授权和启动文件选择器后调用
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // 权限已经授予
                    Log.d("Permission", "Permission granted");
                    openFilePicker();
                } else {
                    Log.d("Permission", "Permission denied");
                    // 用户拒绝了权限请求，处理相应逻辑
                    Toast.makeText(this, "Storage permission is necessary to export contacts!", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_FILE_PICK && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                // 处理选取的文件，例如读取文件内容
                TXTImporter importer = new TXTImporter();
                importer.importFromTXT(getContentResolver(), uri);
            }
        }
    }
    //返回按钮地事件处理
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 结束当前活动并返回
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
