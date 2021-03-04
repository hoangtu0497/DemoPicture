package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.example.myapplication.databinding.FragmentMainBinding;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainFragment extends Fragment {

    private static final int CAMERA_REQUEST_CODE = 9999;
    private FragmentMainBinding binding;
    private String oldPhotoPath;
    private String currentPhotoPath;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initViews();
    }

    private void initViews() {
        binding.btnCaptureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)) {
                        //TODO: Show permission dialog
                    } else {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                    }
                } else {
                    beginTakePicture();
                }
            }
        });

    }

    private void beginTakePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.resolveActivity(requireContext().getPackageManager());

        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (photoFile == null)
            return;

        Uri photoUri = FileProvider.getUriForFile(requireContext(), "custom-authority", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (oldPhotoPath != null) {
                    (new File(oldPhotoPath)).delete();
                    oldPhotoPath = null;
                }
                showCapturedPicture();
            } else {
                (new File(currentPhotoPath)).delete();
                if (oldPhotoPath != null) {
                    currentPhotoPath = oldPhotoPath;
                    oldPhotoPath = null;
                } else {
                    currentPhotoPath = null;
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showCapturedPicture() {
        Bitmap bitmap = null;
        try {
            bitmap = getCurrentBitmap();
        } catch (IOException e) {
            e.printStackTrace();
        }
        binding.ivCaptured.setImageBitmap(bitmap);
    }

    private File createImageFile() throws IOException {
        String timeStamp = (new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())).format(new Date());
        File storageDir = requireContext().getFilesDir();

        if (currentPhotoPath != null)
            oldPhotoPath = currentPhotoPath;

        File createdFile = File.createTempFile(timeStamp, ".jpeg", storageDir);
        currentPhotoPath = createdFile.getAbsolutePath();
        return createdFile;
    }

    private Bitmap rotateImage(Bitmap img, Float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    @Override
    public void onDestroy() {
        if (currentPhotoPath != null)
            (new File(currentPhotoPath)).delete();
        super.onDestroy();
    }

    private Bitmap getCurrentBitmap() throws IOException {
        if (currentPhotoPath == null)
            return null;

        File file = new File(currentPhotoPath);
        ExifInterface exif = new ExifInterface(file.getAbsolutePath());
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        Bitmap img = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), Uri.fromFile(file));
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                img = rotateImage(img, 90f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                img = rotateImage(img, 180f);
                break;
            case 270:
                img = rotateImage(img, 270f);
                break;
            default:
        }

        return img;
    }

}
