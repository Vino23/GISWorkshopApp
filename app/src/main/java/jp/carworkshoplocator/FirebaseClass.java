package jp.carworkshoplocator;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Observable;

public class FirebaseClass extends Observable {
    String Id;
    Context mContext;

    //Other declartions
    String oldEmail;
    ArrayList arrayList = new ArrayList();
    private String updateStatus;

    public FirebaseClass(Context context, String Id){
        this.mContext = context;
        this.Id = Id;
    }
    public FirebaseClass(Context context){
        this.mContext = context;
    }
    public String getEmail(){
        return getCurrentUser().getEmail();
    }
    public String getUid(){
        return getCurrentUser().getUid();
    }
    public FirebaseAuth getUserInstance(){
        return FirebaseAuth.getInstance();
    }
    public FirebaseUser getCurrentUser(){
        return getUserInstance().getCurrentUser();
    }
    public boolean isUserLoggedin(){
        FirebaseUser user = getUserInstance().getCurrentUser();
        if(user != null){
            return true;
        }
        return false;
    }

    public DatabaseReference getDatabaseReference(){
        return FirebaseDatabase.getInstance().getReference();
    }

    public String getId(){
        return Id;
    }

}
