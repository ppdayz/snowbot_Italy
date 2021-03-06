package com.csjbot.snowbot.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.alibaba.fastjson.JSON;
import com.csjbot.snowbot.R;
import com.csjbot.snowbot.app.CsjUIActivity;
import com.csjbot.snowbot.client.NettyClient;
import com.csjbot.snowbot.client.nettyHandler.ClientListener;
import com.csjbot.snowbot.services.FloatingWindowsService;
import com.csjbot.snowbot.utils.SharedPreferencesUtils;
import com.csjbot.snowbot_rogue.bean.PoseListBean;
import com.csjbot.snowbot_rogue.platform.SnowBotManager;
import com.csjbot.snowbot_rogue.servers.slams.MoveServerMapListener;
import com.csjbot.snowbot_rogue.servers.slams.SnowBotMoveServer;
import com.csjbot.snowbot_rogue.utils.CSJToast;
import com.csjbot.snowbot_rogue.utils.SharePreferenceTools;
import com.csjbot.snowbot_rogue.utils.WeakReferenceHandler;
import com.slamtec.slamware.action.MoveDirection;
import com.slamtec.slamware.robot.Location;
import com.slamtec.slamware.robot.Pose;

import java.util.ArrayList;
import java.util.List;


public class MapActivity_backup extends CsjUIActivity implements View.OnTouchListener, ClientListener {

    private ImageView mapImageView;
    private SnowBotManager snowBot = SnowBotManager.getInstance();

    private MapActivityHandler mHandler = new MapActivityHandler(this);
    private Bitmap bm;
    private float mOffsetX, mOffsetY;

    private Pose pose1, pose2, pose3;
    private Bitmap bitmapPos1, bitmapPos2, bitmapPos3, addWallSP;
    //    private PointF point1 = new PointF(), point2 = new PointF(), point3 = new PointF();
    private PointF startP, endP;
    private boolean isAddingWall;
    private ImageView goHome;

    private Paint paint = new Paint();
    private NettyClient client = NettyClient.getInstence();
    private String ipAddr;

    private PoseListBean poseListBean = null;
    private SharePreferenceTools sharePreferenceTools;
    private Intent serviceIntent;

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @param v     The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *              the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float bw = (screenW - imageMapH) / 2;
            if (event.getX() < bw || event.getX() > bw + imageMapH) {
                return false;
            }
            float offsetX = ((bm.getWidth()) * 1f / imageMapH) * (event.getX() - bw),
                    offsetY = (bm.getHeight() * 1f / imageMapH) * event.getY();

            mOffsetX = offsetX;
            mOffsetY = offsetY;

            if (!isAddingWall) {
                snowBot.moveTo(offsetX, offsetY);
            } else {
                // 如果开始点是空，就说明没有开始，设置开始点
                if (startP == null) {
                    startP = new PointF(mOffsetX, mOffsetY);
                } else if (endP == null) {  // 如果结束点为空，开始点不为空，就设置结束点，并且添加一个虚拟墙
                    endP = new PointF(mOffsetX, mOffsetY);
                    snowBot.addWall(startP, endP);
                } else { // 否则就清空
                    startP = null;
                    endP = null;
                }
            }

//            Logger.d("pos = " + event.getX() + " " + event.getY());
        }

        return false;
    }

    private Bitmap superposition(Bitmap oriMap, Bitmap arrow, float offsetX, float offsetY) {

        // 另外创建一张图片
        // 创建一个新的和SRC长度宽度一样的位图
        Bitmap newb = Bitmap.createBitmap(oriMap.getWidth(), oriMap.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(newb);

        canvas.drawBitmap(oriMap, 0, 0, null);// 在 0，0坐标开始画入原图片src

        if (!isAddingWall) {
            canvas.drawBitmap(arrow, offsetX - arrow.getWidth() / 2, offsetY - arrow.getHeight() / 2, null);
        }

        if (pose1 != null) {
            Location L1 = pose1.getLocation();
            canvas.drawBitmap(bitmapPos1, (oriMap.getHeight() - bitmapPos1.getHeight()) / 2 - L1.getY() / 0.05f,
                    (oriMap.getWidth() - bitmapPos1.getWidth()) / 2 - L1.getX() / 0.05f,
                    null);
        }

        if (pose2 != null) {
            Location L2 = pose2.getLocation();
            canvas.drawBitmap(bitmapPos2, (oriMap.getHeight() - bitmapPos2.getHeight()) / 2 - L2.getY() / 0.05f,
                    (oriMap.getWidth() - bitmapPos2.getWidth()) / 2 - L2.getX() / 0.05f,
                    null);
        }
        if (pose3 != null) {
            Location L3 = pose3.getLocation();
            canvas.drawBitmap(bitmapPos3, (oriMap.getHeight() - bitmapPos3.getHeight()) / 2 - L3.getY() / 0.05f,
                    (oriMap.getWidth() - bitmapPos3.getWidth()) / 2 - L3.getX() / 0.05f,
                    null);
        }

        if (startP != null) {
            canvas.drawBitmap(addWallSP, startP.x - addWallSP.getWidth() / 2, startP.y - addWallSP.getHeight() / 2, null);
        }

        if (endP != null) {
            canvas.drawBitmap(addWallSP, endP.x - addWallSP.getWidth() / 2, endP.y - addWallSP.getHeight() / 2, null);
        }

        if (startP != null && endP != null) {
            canvas.drawLine(startP.x, startP.y, endP.x, endP.y, paint);
        }


        //旋转图片 动作
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        arrow.recycle();

        return newb;
    }


    public void goHome(View view) {
        List<Pose> poses = new ArrayList<>();
        if (pose1 != null) {
            poses.add(pose1);
        }
        if (pose2 != null) {
            poses.add(pose2);
        }
        if (pose3 != null) {
            poses.add(pose3);
        }


        snowBot.partol(poses);
//        snowBot.goHome();
    }

    public void setPoint1(View view) {
        pose1 = snowBot.getPose();
        writePoseToDisk();
    }

    public void setPoint2(View view) {
        pose2 = snowBot.getPose();
        writePoseToDisk();
    }

    public void setPoint3(View view) {
        pose3 = snowBot.getPose();
        writePoseToDisk();
    }

    public void goToP1(View view) {
        if (pose1 != null) {
            snowBot.moveTo(pose1.getLocation());
        }
//        snowBot.cancelAction();
    }

    public void goToP2(View view) {
        if (pose2 != null) {
            snowBot.moveTo(pose2.getLocation());
        }

//        snowBot.setDeNoise(false);
    }

    public void goToP3(View view) {
        if (pose3 != null) {
            snowBot.moveTo(pose3.getLocation());
        }

//        snowBot.setDeNoise(true);
    }

    public void clearMap(View view) {
        pose1 = null;
        pose2 = null;
        pose3 = null;
        poseListBean.getPoses().clear();
        writePoseToDisk();
        snowBot.clearMap();
    }

    public void addWall(View view) {
        if (isAddingWall) {
            ((ImageView) view).setImageResource(R.mipmap.add_wall);
            isAddingWall = false;
            CSJToast.showToast(this, getResources().getString(R.string.cancle_add_wall), 1000);
            startP = null;
            endP = null;
        } else {
            ((ImageView) view).setImageResource(R.mipmap.adding_wall);
            isAddingWall = true;
            CSJToast.showToast(this, getResources().getString(R.string.start_add_wall), 1000);

        }
    }

    public void clearWall(View view) {
        snowBot.clearWalls();
    }

    @Override
    public void recMessage(final String msg) {
//        Logger.d(msg);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                CSJToast.showToast(MapActivity_backup.this, msg, 2000);
            }
        });

        switch (msg) {
            case "setpos1":
                setPoint1(null);
                break;
            case "setpos2":
                setPoint2(null);
                break;
            case "setpos3":
                setPoint3(null);
                break;
            case "gotopos1":
                goToP1(null);
                break;
            case "gotopos2":
                goToP2(null);
                break;
            case "gotopos3":
                goToP3(null);
                break;
            case "stop":
                snowBot.cancelAction();
                break;
            case "up":
                snowBot.moveBy(MoveDirection.FORWARD);
                break;
            case "down":
                snowBot.moveBy(MoveDirection.BACKWARD);
                break;
            case "left":
                snowBot.moveBy(MoveDirection.TURN_LEFT);
                break;
            case "right":
                snowBot.moveBy(MoveDirection.TURN_RIGHT);
                break;
            default:
                break;
        }
    }

    @Override
    public void clientConnected() {
        SharedPreferencesUtils.setParam(this, "ip_addr", ipAddr);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                CSJToast.showToast(MapActivity_backup.this, getResources().getString(R.string.IP_save) + ipAddr);
            }
        });
    }

    @Override
    public void clientDisConnected() {

    }

    public void mapFinished(View view) {
        finish();
    }

    @Override
    public void afterViewCreated(Bundle savedInstanceState) {
        mapImageView = (ImageView) findViewById(R.id.mapImageView);

        sharePreferenceTools = new SharePreferenceTools(this);
        String postListString = sharePreferenceTools.getString("PoseListBean", "");
        poseListBean = JSON.parseObject(postListString, PoseListBean.class);

        initPose();
        serviceIntent = new Intent(this, FloatingWindowsService.class);
        startService(serviceIntent);

        snowBot.setMoveServerMapListener(new MoveServerMapListener() {
            @Override
            public void getMap(Bitmap map) {
                Message msg = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putParcelable("bitmap", map);
                msg.setData(bundle);
                msg.what = 0x00;

                mHandler.sendMessage(msg);
            }


        });
        snowBot.connect();
        snowBot.getMap(this);

//        ConnectHandler.setListener(this);

        bitmapPos1 = BitmapFactory.decodeResource(getResources(), R.mipmap.location_1);
        bitmapPos2 = BitmapFactory.decodeResource(getResources(), R.mipmap.location_2);
        bitmapPos3 = BitmapFactory.decodeResource(getResources(), R.mipmap.location_3);
        addWallSP = BitmapFactory.decodeResource(getResources(), R.mipmap.add_wall_start_point);
        paint.setColor(Color.BLUE);

        mapImageView.setOnTouchListener(this);
        ViewTreeObserver vto2 = mapImageView.getViewTreeObserver();
        vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mapImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                Logger.d("" + mapImageView.getHeight() + "," + mapImageView.getWidth());
                imageMapW = mapImageView.getWidth();
                imageMapH = mapImageView.getHeight();
            }
        });

        WindowManager wm = this.getWindowManager();

        screenW = wm.getDefaultDisplay().getWidth();
        screenH = wm.getDefaultDisplay().getHeight();

        goHome = (ImageView) findViewById(R.id.goHome);

        goHome.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                EditText text = new EditText(MapActivity_backup.this);
                text.setText((String) SharedPreferencesUtils.getParam(MapActivity_backup.this, "ip_addr", "192.168.12.105"));

                new MaterialDialog.Builder(MapActivity_backup.this)
                        .title(R.string.title)
                        .customView(text, false)
                        .positiveText(getResources().getString(R.string.connect_text))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                EditText editText = (EditText) dialog.getCustomView();
                                if (editText != null) {
                                    ipAddr = editText.getText().toString();
                                    SnowBotMoveServer.getInstance().connect();
                                    SharedPreferencesUtils.setParam(MapActivity_backup.this, "ip_addr", ipAddr);
//                                    Logger.d(editText.getText().toString());
                                }
                            }
                        })
                        .show();
                return false;
            }
        });
    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_map;
    }

    static class MapActivityHandler extends WeakReferenceHandler<MapActivity_backup> {

        public MapActivityHandler(MapActivity_backup reference) {
            super(reference);
        }

        @Override
        protected void handleMessage(MapActivity_backup reference, Message msg) {
            switch (msg.what) {
                case 0x00:
                    reference.bm = msg.getData().getParcelable("bitmap");
                    Bitmap bm = reference.superposition(reference.bm,
                            BitmapFactory.decodeResource(reference.getResources(), R.mipmap.go_point),
                            reference.mOffsetX, reference.mOffsetY);
                    reference.mapImageView.setImageBitmap(bm);
                    break;
                default:
                    break;
            }
        }

    }

    private int imageMapW, imageMapH, screenW, screenH;

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_map);
//        mapImageView = (ImageView) findViewById(R.id.mapImageView);
//
//        sharePreferenceTools = new SharePreferenceTools (this);
//        String postListString = sharePreferenceTools.getString("PoseListBean", "");
//        poseListBean = JSON.parseObject(postListString, PoseListBean.class);
//
//        initPose();
//        serviceIntent = new Intent(this, FloatingWindowsService.class);
//        startService(serviceIntent);
//
//        snowBot.setMoveServerMapListener(new MoveServerMapListener() {
//            @Override
//            public void getMap(Bitmap map) {
//                Message msg = mHandler.obtainMessage();
//                Bundle bundle = new Bundle();
//                bundle.putParcelable("bitmap", map);
//                msg.setData(bundle);
//                msg.what = 0x00;
//
//                mHandler.sendMessage(msg);
//            }
//
//
//        });
//        snowBot.connect();
//        snowBot.getMap(this);
//
////        ConnectHandler.setListener(this);
//
//        bitmapPos1 = BitmapFactory.decodeResource(getResources(), R.mipmap.location_1);
//        bitmapPos2 = BitmapFactory.decodeResource(getResources(), R.mipmap.location_2);
//        bitmapPos3 = BitmapFactory.decodeResource(getResources(), R.mipmap.location_3);
//        addWallSP = BitmapFactory.decodeResource(getResources(), R.mipmap.add_wall_start_point);
//        paint.setColor(Color.BLUE);
//
//        mapImageView.setOnTouchListener(this);
//        ViewTreeObserver vto2 = mapImageView.getViewTreeObserver();
//        vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                mapImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
////                Logger.d("" + mapImageView.getHeight() + "," + mapImageView.getWidth());
//                imageMapW = mapImageView.getWidth();
//                imageMapH = mapImageView.getHeight();
//            }
//        });
//
//        WindowManager wm = this.getWindowManager();
//
//        screenW = wm.getDefaultDisplay().getWidth();
//        screenH = wm.getDefaultDisplay().getHeight();
//
//        goHome = (ImageView) findViewById(R.id.goHome);
//
//        goHome.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                EditText text = new EditText(MapActivity_backup.this);
//                text.setText((String) SharedPreferencesUtils.getParam(MapActivity_backup.this, "ip_addr", "192.168.12.105"));
//
//                new MaterialDialog.Builder(MapActivity_backup.this)
//                        .title(R.string.title)
//                        .customView(text, false)
//                        .positiveText("连接")
//                        .onPositive(new MaterialDialog.SingleButtonCallback() {
//                            @Override
//                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                                EditText editText = (EditText) dialog.getCustomView();
//                                if (editText != null) {
//                                    ipAddr = editText.getText().toString();
//                                    SnowBotMoveServer.getInstance().connect(editText.getText().toString(), false);
//                                    SharedPreferencesUtils.setParam(MapActivity_backup.this, "ip_addr", ipAddr);
//                                    Logger.d(editText.getText().toString());
//                                }
//                            }
//                        })
//                        .show();
//                return false;
//            }
//        });
//    }

    private void writePoseToDisk() {
        List<PoseListBean.PosesBean> poses = poseListBean.getPoses();
        poses.clear();

        if (pose1 != null) {
            poses.add(new PoseListBean.PosesBean(1, pose1.getX(), pose1.getY(), pose1.getYaw()));
        }

        if (pose2 != null) {
            poses.add(new PoseListBean.PosesBean(2, pose2.getX(), pose2.getY(), pose2.getYaw()));
        }

        if (pose3 != null) {
            poses.add(new PoseListBean.PosesBean(3, pose3.getX(), pose3.getY(), pose3.getYaw()));
        }

        String json = JSON.toJSONString(poseListBean);
        sharePreferenceTools.putString("PoseListBean", json);
    }

    private void initPose() {
        if (poseListBean == null) {
            poseListBean = new PoseListBean();
        }
        List<PoseListBean.PosesBean> poses = poseListBean.getPoses();
        for (PoseListBean.PosesBean pose : poses) {
            switch (pose.getIndex()) {
                case 1:
                    pose1 = new Pose();
                    pose1.setX(pose.getLocation().getX());
                    pose1.setY(pose.getLocation().getY());
                    pose1.setYaw(pose.getRotation().getYaw());
                    break;
                case 2:
                    pose2 = new Pose();
                    pose2.setX(pose.getLocation().getX());
                    pose2.setY(pose.getLocation().getY());
                    pose2.setYaw(pose.getRotation().getYaw());
                    break;
                case 3:

                    pose3 = new Pose();
                    pose3.setX(pose.getLocation().getX());
                    pose3.setY(pose.getLocation().getY());
                    pose3.setYaw(pose.getRotation().getYaw());
                    break;
                default:
            }
        }
    }

    @Override
    protected void onDestroy() {
        snowBot.stopGetMap();
        snowBot.stopPartol();
//        if (serviceIntent != null) {
//            stopService(serviceIntent);
//        }
        super.onDestroy();
    }
}
