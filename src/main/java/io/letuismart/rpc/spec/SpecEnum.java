package io.letuismart.rpc.spec;

public enum SpecEnum {
    Q_BEAN("Q-BEAN"),
    Q_METHOD("Q-METHOD"),
    Q_PARAM("Q-PARAM-");

    private String value;

    public String val(){
        return this.value;
    }

    SpecEnum(String value){
        this.value=value;
    }
}
