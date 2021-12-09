package com.bistu.ct.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class MainActivity extends BaseActivity implements View.OnClickListener{
    ImageView nextIv,playIv,lastIv,albumIv;
    TextView singerTv,songTv;
    TextView musicLength, musicCur;
    RecyclerView musicRv;
    EditText searchEt;
    ImageView submitIv;
    String song;
    SeekBar seekBar;
    //查到的歌曲id
    int songId;
    //查到的歌曲下载url
    String songUrl;
    //标识查询的是哪一次
    int flag=0;
    //数据源
    public static List<MusicBean>mDatas;
    public static  MusicAdapter adapter;
    //记录当前正在播放的音乐的位置
    int currentPlayPosition=-1;
    //记录暂停音乐进度条的位置
    int currentPausePositionInSong=0;
    private Timer timer;
    MediaPlayer mediaPlayer;
    SimpleDateFormat format;
    MusicBean bean =new MusicBean();
    private boolean isSeekBarChanging;//互斥变量，防止进度条与定时器冲突。
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkNeedPermissions();
        initView();
        mediaPlayer = new MediaPlayer();

        mDatas = new ArrayList<>();
        //创建适配器
        adapter = new MusicAdapter(this,mDatas);
        musicRv.setAdapter(adapter);
//      设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        musicRv.setLayoutManager(layoutManager);
        //加载本地数据源
        loadMusicData();
        //设置每一项的点击事件
        setEventListener();

        format = new SimpleDateFormat("mm:ss");
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                seekBar.setMax(mediaPlayer.getDuration());
                musicLength.setText(format.format(mediaPlayer.getDuration()) + "");
                musicCur.setText("00:00");
                System.out.println(musicCur);
            }

    } );
    }

    private void setEventListener() {
        //设置每一项的点击事件
        adapter.setOnItemClickLister(new MusicAdapter.OnItemClickLister() {
            @Override
            public void OnItemClick(View view, int position) throws IOException {
                currentPlayPosition = position;
                MusicBean musicBean = mDatas.get(position);
                playMusicInMusicBean(musicBean);
            }


        });
    }

    public void playMusicInMusicBean(MusicBean musicBean) {
        /*根据传入对象播放音乐*/
        //设置底部显示的歌手名称和歌曲名
        singerTv.setText(musicBean.getSinger());
        songTv.setText(musicBean.getSong());
        stopMusic();
//                重置多媒体播放器
        mediaPlayer.reset();
//                设置新的播放路径
        try {
            mediaPlayer.setDataSource(musicBean.getPath());
            String albumArt = musicBean.getAlbumArt();
            Log.i("lsh123", "playMusicInMusicBean: albumpath=="+albumArt);
            Bitmap bm = BitmapFactory.decodeFile(albumArt);
            Log.i("lsh123", "playMusicInMusicBean: bm=="+bm);
            albumIv.setImageBitmap(bm);
            playMusic();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    点击播放按钮/从暂停音乐到重新播放
     */
    private void playMusic() {
        //播放音乐
        if(mediaPlayer!=null&&!mediaPlayer.isPlaying()){
            if(currentPausePositionInSong == 0){
                try {
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    seekBar.setMax(mediaPlayer.getDuration());
                    musicLength.setText(format.format(mediaPlayer.getDuration()) + "");
                    musicCur.setText("00:00");

                    mediaPlayer.seekTo(currentPlayPosition);

                    //监听播放时回调函数
                    timer = new Timer();
                    timer.schedule(new TimerTask() {

                        Runnable updateUI = new Runnable() {
                            @Override
                            public void run() {
                                musicCur.setText(format.format(mediaPlayer.getCurrentPosition()) + "");
                            }
                        };

                        @Override
                        public void run() {
                            if (!isSeekBarChanging) {
                                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                                runOnUiThread(updateUI);
                            }
                        }
                    }, 0, 50);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }else{
                //从暂停到播放
                mediaPlayer.seekTo(currentPausePositionInSong);
                mediaPlayer.start();
                seekBar.setMax(mediaPlayer.getDuration());
                musicLength.setText(format.format(mediaPlayer.getDuration()) + "");
                musicCur.setText(String.valueOf(currentPausePositionInSong));
                mediaPlayer.seekTo(currentPausePositionInSong);
                //监听播放时回调函数
                timer = new Timer();
                timer.schedule(new TimerTask() {

                    Runnable updateUI = new Runnable() {
                        @Override
                        public void run() {
                            musicCur.setText(format.format(mediaPlayer.getCurrentPosition()) + "");
                        }
                    };

                    @Override
                    public void run() {
                        if (!isSeekBarChanging) {
                            seekBar.setProgress(mediaPlayer.getCurrentPosition());
                            runOnUiThread(updateUI);
                        }
                    }
                }, 0, 50);
            }
            playIv.setImageResource(R.mipmap.icon_pause);
        }


    }

    private void pauseMusic() {
        //暂停音乐的函数
        if(mediaPlayer!=null&&mediaPlayer.isPlaying()){
            currentPausePositionInSong = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            playIv.setImageResource(R.mipmap.icon_play);
        }
    }

    private void stopMusic() {
        //停止音乐
        if(mediaPlayer!=null){
            currentPausePositionInSong = 0;
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            mediaPlayer.stop();
            playIv.setImageResource(R.mipmap.icon_play);
        }
    }

    protected void onDestroy(){
        MainActivity.super.onDestroy();
        stopMusic();
    }

    private void loadMusicData() {
        String sid=null;
        //加载本地存储当中的mp3文件到集合当中
        //1.获取ContentResolver对象
        ContentResolver resolver = getContentResolver();
        //2.获取本地音乐地址
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        //3.开始查询地址
        Cursor cursor = resolver.query(uri,null,null,null,null);
        //4.遍历Cursor
        int id = 0;
        while (cursor.moveToNext()){
            String song = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String singer = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            id++;
            sid = String.valueOf(id);
            String  path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
            System.out.println(path);
            long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
            String time  = sdf.format(new Date(duration));
            //          获取专辑图片主要是通过album_id进行查询
            @SuppressLint("Range") String album_id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
            String albumArt = getAlbumArt(album_id);
            //将第一行数据封装到对象中
            MusicBean bean = new MusicBean(sid,song,singer,album,time,path,albumArt);
            mDatas.add(bean);
        }
        Vector<File> mp3 = getAllFiles(Environment.getExternalStorageDirectory()+"/Music", "mp3");
        System.out.println(Environment.getDataDirectory().getAbsolutePath()+"/Music");
        for(int i=0;i<mp3.size();i++){
            String song = mp3.get(i).getName();
            String path = mp3.get(i).getPath();
            sid = String.valueOf(Integer.valueOf(sid)+1);
            MusicBean bean = new MusicBean(sid,song,"","","",path,"");
            mDatas.add(bean);
        }
//        数据源发生变化，提示更新
        adapter.notifyDataSetChanged();
    }

    private String getAlbumArt(String album_id) {
        String mUriAlbums = "content://media/external/audio/albums";
        String[] projection = new String[]{"album_art"};
        Cursor cur = this.getContentResolver().query(
                Uri.parse(mUriAlbums + "/" + album_id),
                projection, null, null, null);
        String album_art = null;
        if (cur.getCount() > 0 && cur.getColumnCount() > 0) {
            cur.moveToNext();
            album_art = cur.getString(0);
        }
        cur.close();
        cur = null;
        return album_art;
    }

    private void initView() {
        //初始化控件
        nextIv = findViewById(R.id.local_music_bottom_iv_next);
        playIv = findViewById(R.id.local_music_bottom_iv_play);
        lastIv = findViewById(R.id.local_music_bottom_iv_last);
        singerTv = findViewById(R.id.local_music_bottom_tv_singer);
        songTv = findViewById(R.id.local_music_bottom_tv_song);
        musicRv = findViewById(R.id.local_music_rv);
        albumIv = findViewById(R.id.phonto);
        searchEt = findViewById(R.id.search_input);
        submitIv = findViewById(R.id.search_submit);
        seekBar = findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new MySeekBar());
        musicLength = (TextView) findViewById(R.id.music_length);
        musicCur = (TextView) findViewById(R.id.music_cur);
        nextIv.setOnClickListener(this);
        playIv.setOnClickListener(this);
        lastIv.setOnClickListener(this);
        submitIv.setOnClickListener(this);

    }


public class MySeekBar implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            if(mediaPlayer.getCurrentPosition() ==mediaPlayer.getDuration()){
                if(currentPlayPosition==mDatas.size()-1){
                    Toast.makeText(MainActivity.this,"已经是最后一首了，没有下一曲",Toast.LENGTH_SHORT).show();
                    return;
                }
                currentPlayPosition = currentPlayPosition+1;
                MusicBean nextBean = mDatas.get(currentPlayPosition);
                playMusicInMusicBean(nextBean);
            }
        }

        /*滚动时,应当暂停后台定时器*/
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = true;
        }

        /*滑动结束后，重新设置值*/
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = false;
            mediaPlayer.seekTo(seekBar.getProgress());
            if(mediaPlayer.getCurrentPosition() ==mediaPlayer.getDuration()){
                if(currentPlayPosition==mDatas.size()-1){
                    Toast.makeText(MainActivity.this,"已经是最后一首了，没有下一曲",Toast.LENGTH_SHORT).show();
                    return;
                }
                currentPlayPosition = currentPlayPosition+1;
                MusicBean nextBean = mDatas.get(currentPlayPosition);
                playMusicInMusicBean(nextBean);
            }

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.local_music_bottom_iv_last:
                if(currentPlayPosition==0){
                    Toast.makeText(this,"已经是第一首了，没有上一曲",Toast.LENGTH_SHORT).show();
                    return;
                }
                currentPlayPosition = currentPlayPosition-1;
                MusicBean lastBean = mDatas.get(currentPlayPosition);
                playMusicInMusicBean(lastBean);

                break;
            case R.id.local_music_bottom_iv_play:
                if(currentPlayPosition == -1){
                    //并没有选中要播放的音乐
                    Toast.makeText(this,"请选择要播放的音乐",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mediaPlayer.isPlaying()){
                    //此时处于播放状态，需要暂停音乐
                    pauseMusic();
                }else{
                    //此时没有播放音乐，点击开始播放
                    playMusic();


                }
                break;
            case R.id.local_music_bottom_iv_next:
                if(currentPlayPosition==mDatas.size()-1){
                    Toast.makeText(this,"已经是最后一首了，没有下一曲",Toast.LENGTH_SHORT).show();
                    return;
                }
                currentPlayPosition = currentPlayPosition+1;
                MusicBean nextBean = mDatas.get(currentPlayPosition);
                playMusicInMusicBean(nextBean);
                break;
            case R.id.search_submit:
                songUrl=null; //先初始化
                song = searchEt.getText().toString();
                if (!TextUtils.isEmpty(song)) {
//                      判断是否能够找到这个歌曲
                    String SongId= URLUtil.url1+song;
                    loadData(SongId);
                    System.out.println(SongId);

                }else {
                    Toast.makeText(this,"输入内容不能为空！",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.seek_bar:

                break;
        }
        }

    /**
     * 成功获取到返回值时调用
     * @param result
     */
    @Override
    public void onSuccess(String result) {
        if(flag== 0){
            json2 json2 = new Gson().fromJson(result, com.bistu.ct.musicplayer.json2.class);
            songId = json2.getResult().getSongs().get(0).getId();
            System.out.println(songId);
            loadData(URLUtil.getUrl3(songId));
            flag = 1;
        }else {
            System.out.println("111");
            json json= new Gson().fromJson(result, com.bistu.ct.musicplayer.json.class);
            songUrl = json.getData().get(0).getUrl();
            System.out.println(songUrl);
            if(songUrl!=null){
                Intent intent=new Intent(MainActivity.this,DownLoadService.class);
                intent.putExtra("download_url",songUrl);
                intent.putExtra("song",song);
                startService(intent);
            }else{
                Toast.makeText(this,"暂时无权限下载该歌曲",Toast.LENGTH_SHORT).show();
            }
          flag = 0; //还原标志位
        }
    }

    /**
     * 获取权限
     */
    private void checkNeedPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //多个权限一起申请
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }
    }
    /**
     * 获取指定目录内所有文件路径
     * @param dirPath 需要查询的文件目录
     * @param fileType   查询类型，比如mp3什么的
     */
    public Vector<File> getAllFiles(String dirPath, String fileType) {
        Vector<File> fileVector = new Vector<>();
        File f = new File(dirPath);
        if (!f.exists()) {//判断路径是否存在
            return fileVector;
        }
        File[] files = f.listFiles();
        if (files == null) {//判断权限
            return fileVector;
        }
        Vector<File> vecFile = new Vector<File>();
        for (File _file : files) {//遍历目录
            if (_file.isFile() && _file.getName().endsWith(fileType)) {
                vecFile.add(_file);
            } else if (_file.isDirectory()) {//查询子目录
                getAllFiles(_file.getAbsolutePath(), fileType);
            } else {
            }
        }
        return vecFile;
    }
}