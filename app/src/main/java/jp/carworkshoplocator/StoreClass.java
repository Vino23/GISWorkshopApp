package jp.carworkshoplocator;

/**
 * Created by JP on 10/24/2016.
 */
public class StoreClass {

    public String storeLat;
    public String storeLong;
    public String storeOwner;
    public String storeService;

    public StoreClass(){

    }

    public StoreClass(String storeLat, String storeLong, String storeOwner, String storeService){
        this.storeLat = storeLat;
        this.storeLong = storeLong;
        this.storeOwner = storeOwner;
        this.storeService = storeService;
    }

    public String getStoreLat() {
        return storeLat;
    }

    public void setStoreLat(String storeLat) {
        this.storeLat = storeLat;
    }

    public String getStoreLong() {
        return storeLong;
    }

    public void setStoreLong(String storeLong) {
        this.storeLong = storeLong;
    }

    public String getStoreOwner() {
        return storeOwner;
    }

    public void setStoreOwner(String storeOwner) {
        this.storeOwner = storeOwner;
    }

    public String getStoreService() {
        return storeService;
    }

    public void setStoreService(String storeService) {
        this.storeService = storeService;
    }

}
