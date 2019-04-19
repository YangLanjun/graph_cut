package cn.yanglj65;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;

public class DrawingImageView extends android.support.v7.widget.AppCompatImageView {
    private Paint paint;
    private Line currentLine=new Line();
    private ArrayList<Line> lines = new ArrayList<>();

    public DrawingImageView(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        float clickX = event.getX();
        float clickY = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            invalidate();
            Log.i("位置：",clickX+","+clickY);
            return true;
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE)
        {
            Point point = new Point(clickX,clickY);
            //在移动时添加所经过的点
            currentLine.points.add(point);
            invalidate();
            return true;
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            //添加画过的线
            lines.add(currentLine);
            currentLine = new Line();
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        for(int i=0;i<lines.size();i++){
            drawLine(canvas,lines.get(i));
        }
        drawLine(canvas,currentLine);
    }

    private void drawLine(Canvas canvas,Line line){
        paint=new Paint(Paint.DITHER_FLAG);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(5);
        for (int i = 0; i < line.points.size() - 1; i++)
        {
            float x = line.points.get(i).x;
            float y = line.points.get(i).y;

            float nextX = line.points.get(i + 1).x;
            float nextY = line.points.get(i + 1).y;

            canvas.drawLine(x, y, nextX, nextY, paint);
        }


    }
}
