package com.trysendemailphoto;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.creativityapps.gmailbackgroundlibrary.BackgroundMail;
import com.ragnarok.rxcamera.RxCamera;
import com.ragnarok.rxcamera.RxCameraData;
import com.ragnarok.rxcamera.config.CameraUtil;
import com.ragnarok.rxcamera.config.RxCameraConfig;
import com.ragnarok.rxcamera.request.Func;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "EmailWithOutUI";
    @BindView(R.id.edEmail)
    EditText edEmail;
    @BindView(R.id.edMessage)
    EditText edMessage;
    @BindView(R.id.button)
    Button button;
    @BindView(R.id.preview_surface)
    TextureView previewSurface;
    private String password = "roy64389304";
    private String formEmail = "roy.leung@gravitas.com.hk";
    private RxCamera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        openCamera();
    }

    @OnClick(R.id.button)
    public void onClick() {


        centerFocusAndTakePhoto(previewSurface, camera);

    }

    private void sendEmail(String attachmentPath, String toEmail, String message, String subject) {
        BackgroundMail.Builder builder = BackgroundMail.newBuilder(this)
                .withUsername(formEmail)
                .withPassword(password)
                .withMailto(toEmail)
                .withType(BackgroundMail.TYPE_HTML)
                .withSubject(subject)
                .withBody(message)
                .withOnSuccessCallback(new BackgroundMail.OnSuccessCallback() {
                    @Override
                    public void onSuccess() {
                        //do some magic
                        Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                    }
                })
                .withOnFailCallback(new BackgroundMail.OnFailCallback() {
                    @Override
                    public void onFail() {
                        //do some magic
                        Toast.makeText(MainActivity.this, "Fail", Toast.LENGTH_SHORT).show();
                    }
                });
        if (attachmentPath != null) {
            builder.withAttachments(attachmentPath);
        }
        builder.send();

    }

    private void openCamera() {
        RxCameraConfig config = new RxCameraConfig.Builder()
                .useBackCamera()
                .setAutoFocus(true)
                .setPreferPreviewFrameRate(15, 30)
                .setPreferPreviewSize(new Point(640, 480), false)
                .setHandleSurfaceEvent(true)
                .build();
        Log.d(TAG, "config: " + config);
        RxCamera.open(this, config).flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
            @Override
            public Observable<RxCamera> call(RxCamera rxCamera) {

                camera = rxCamera;
                return rxCamera.bindTexture(previewSurface);
            }
        }).flatMap(new Func1<RxCamera, Observable<RxCamera>>() {
            @Override
            public Observable<RxCamera> call(RxCamera rxCamera) {

                return rxCamera.startPreview();
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<RxCamera>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "open camera error: " + e.getMessage());
            }

            @Override
            public void onNext(final RxCamera rxCamera) {
                camera = rxCamera;
                button.setEnabled(true);
            }
        });
    }

    private boolean checkCamera() {
        if (camera == null || !camera.isOpenCamera()) {
            return false;
        }
        return true;
    }

    private void requestTakePicture() {
        if (!checkCamera()) {
            return;
        }
        camera.request().takePictureRequest(true, new Func() {
            @Override
            public void call() {

            }
        }, 1920, 1080, ImageFormat.JPEG, true).subscribe(new Action1<RxCameraData>() {
            @Override
            public void call(RxCameraData rxCameraData) {

                Date date = new Date();
                String path = Environment.getExternalStorageDirectory() + "/" + date.getTime() + ".jpg";
                File file = new File(path);
                Bitmap bitmap = BitmapFactory.decodeByteArray(rxCameraData.cameraData, 0, rxCameraData.cameraData.length);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                        rxCameraData.rotateMatrix, false);
                try {
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                showLog("Save file on " + path);
                StaticMethod.addToGallery(file, MainActivity.this);
                Toast.makeText(MainActivity.this, "Save file on " + path, Toast.LENGTH_SHORT).show();

                sendEmail(file.getAbsolutePath(), edEmail.getText().toString(), edMessage.getText().toString(), "Testing subject");
            }
        });
    }


    private void centerFocusAndTakePhoto(View previewview, RxCamera camera) {
        float centreX = previewview.getX() + previewview.getWidth() / 2;
        float centreY = previewview.getY() + previewview.getHeight() / 2;
        final Rect rect = CameraUtil.transferCameraAreaFromOuterSize(new Point((int) centreX, (int) centreY),
                new Point(previewview.getWidth(), previewview.getHeight()), 100);
        List<Camera.Area> areaList = Collections.singletonList(new Camera.Area(rect, 1000));
        Observable.zip(camera.action().areaFocusAction(areaList),
                camera.action().areaMeterAction(areaList),
                new Func2<RxCamera, RxCamera, Object>() {
                    @Override
                    public Object call(RxCamera rxCamera, RxCamera rxCamera2) {
                        return rxCamera;
                    }
                }).subscribe(new Subscriber<Object>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
//                showLog("area focus and metering failed: " + e.getMessage());
            }

            @Override
            public void onNext(Object o) {
//                showLog(String.format("area focus and metering success, x: %s, y: %s, area: %s", x, y, rect.toShortString()));
                requestTakePicture();
            }
        });
    }

}
