package com.hijacker;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import static com.hijacker.MainActivity.FRAGMENT_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.copy;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.isolate;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.startAirodump;
import static com.hijacker.MainActivity.stop;
import static com.hijacker.MainActivity.wpa_thread;

public class IsolatedFragment extends Fragment{
    View view;
    static AP is_ap;
    static TextView essid, manuf, mac, sec1, numbers, sec2;
    static Thread thread;
    static boolean cont = true;
    static int exit_on;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        view = inflater.inflate(R.layout.isolated_fragment, container, false);

        thread = new Thread(new Runnable(){
            @Override
            public void run(){
                cont = true;
                while(cont){
                    try{
                        Thread.sleep(1000);
                        refresh.obtainMessage().sendToTarget();
                    }catch(InterruptedException ignored){}
                }
            }
        });

        essid = (TextView)view.findViewById(R.id.essid);
        manuf = (TextView)view.findViewById(R.id.manuf);
        mac = (TextView)view.findViewById(R.id.mac);
        sec1 = (TextView)view.findViewById(R.id.sec1);
        numbers = (TextView)view.findViewById(R.id.numbers);
        sec2 = (TextView)view.findViewById(R.id.sec2);

        ListView listview = (ListView)view.findViewById(R.id.listview);
        listview.setAdapter(MainActivity.adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, final View view, int i, long l){
                final Item clicked = Item.items.get(i);

                //ST
                PopupMenu popup = new PopupMenu(getActivity(), view);
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                popup.getMenu().add(0, 0, 0, "Info");
                popup.getMenu().add(0, 1, 1, "Copy MAC");
                if(clicked.st.bssid!=null){
                    popup.getMenu().add(0, 2, 2, "Disconnect");
                    popup.getMenu().add(0, 3, 3, "Copy disconnect command");
                }

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(android.view.MenuItem item) {
                        switch(item.getItemId()) {
                            case 0:
                                //Info
                                STDialog dialog = new STDialog();
                                dialog.info_st = clicked.st;
                                dialog.show(getFragmentManager(), "STDialog");
                                break;
                            case 1:
                                //copy to clipboard
                                copy(clicked.st.mac, view);
                                break;
                            case 2:
                                //Disconnect this
                                clicked.st.disconnect();
                                break;
                            case 3:
                                //copy disconnect command to clipboard
                                String str = prefix + " " + aireplay_dir + " --ignore-negative-one --deauth 0 -a " + clicked.st.bssid + " -c " + clicked.st.mac + " " + iface;
                                copy(str, view);
                                break;
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

        return view;
    }
    public static Handler refresh = new Handler(){
        public void handleMessage(Message msg){
            if(cont && is_ap !=null){
                essid.setText(is_ap.essid);
                manuf.setText(is_ap.manuf);
                mac.setText(is_ap.mac);
                sec1.setText("Enc: " + is_ap.enc + " | Auth: " + is_ap.auth);
                numbers.setText("B: " + is_ap.beacons + " | D: " + is_ap.data + " | #s: " + is_ap.ivs);
                sec2.setText("Cipher: " + is_ap.cipher);
            }
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_AIRODUMP;
        thread.start();
        ((Button)view.findViewById(R.id.crack)).setText(wpa_thread.isAlive() ? R.string.stop : R.string.crack);
        ((Button)view.findViewById(R.id.dos)).setText(MDKFragment.ados ? R.string.stop : R.string.dos);
        refresh.obtainMessage().sendToTarget();
    }
    @Override
    public void onPause(){
        super.onPause();
        cont = false;
        thread.interrupt();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        cont = false;
        if(getFragmentManager().getBackStackEntryCount()==exit_on){
            isolate(null);
            stop(PROCESS_AIRODUMP);
            stop(PROCESS_AIREPLAY);
            startAirodump(null);
        }
    }
}
