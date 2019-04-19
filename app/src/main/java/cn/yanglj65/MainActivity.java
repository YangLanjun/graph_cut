package cn.yanglj65;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Button doCutButton;
    private ImageView imageViewSrc;
    private Bitmap imgSrcBitMap;
    private Bitmap imgSrcBitMapCopy;
    private ImageView imageViewDst;
    private Paint paint;
    private Uri imageUri;
    boolean in=false;
    private Line currentLine = new Line();
    private ArrayList<Line> lines = new ArrayList<>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        doCutButton = findViewById(R.id.doGraphCut);
        imageViewSrc = findViewById(R.id.imageSrc);
        doCutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "开始执行图片分割", Toast.LENGTH_SHORT).show();
            }
        });
        imageViewSrc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chooseInAlbum = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(chooseInAlbum, 0);
            }
        });
        imageViewSrc.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (imgSrcBitMap == null&&event.getAction() == MotionEvent.ACTION_DOWN) {
                    Intent chooseInAlbum = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(chooseInAlbum, 0);
                    return false;
                }
                if(imgSrcBitMap!=null){
                    float clickX = event.getX();
                    float clickY = event.getY();
                    float[] dst=new float[2];
                    Matrix imageViewSrcMatrix = imageViewSrc.getImageMatrix();
                    Matrix inverseMatrix = new Matrix();
                    imageViewSrcMatrix.invert(inverseMatrix);
                    inverseMatrix.mapPoints(dst,new float[]{clickX,clickY});
                    Point point = new Point(dst[0], dst[1]);
                    //在移动时添加所经过的点
                    currentLine.points.add(point);
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        Log.i("位置：", dst[0] + "," + dst[1]);
                        return true;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        return true;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        //添加画过的线
                        lines.add(currentLine);
                        drawImage();
                        Log.i("位置：", dst[0] + "," + dst[1]);
                        currentLine = new Line();
                    }
                }

                return false;
            }
        });
    }

    private void drawImage() {
        Canvas canvas = new Canvas(imgSrcBitMapCopy);
        paint = new Paint(Paint.DITHER_FLAG);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(10);
        Point startPos = currentLine.points.get(0);
        Point endPos = currentLine.points.get(currentLine.points.size() - 1);
        canvas.drawLine(startPos.x, startPos.y, endPos.x, endPos.y, paint);
        canvas.save();
        imageViewSrc.setImageBitmap(imgSrcBitMapCopy);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            //从相册获取头像
            case 0:
                if (resultCode == RESULT_OK) {
                    try {
                        imageUri = data.getData();
                        imgSrcBitMap = BitmapFactory.decodeStream(this.getContentResolver().openInputStream(imageUri));
                        imageViewSrc.setImageBitmap(imgSrcBitMap);
                        imgSrcBitMapCopy = imgSrcBitMap.copy(Bitmap.Config.ARGB_8888, true);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
}
