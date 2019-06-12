package cn.edu.bupt.message;

public class IRAction {
    private Integer action;
    public IRAction(Integer action){
        this.action = action;
    }
    public enum IRActionEnum{
        ACTION_RSP_NEW_DEVICE,
        ACTION_IR_STUDY_DATA_RSP,

    }
}
