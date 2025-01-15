package cn.lingque.console.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private int code;
    private String msg;
    private Object data;
    public static Result ok(Object data) {
        return new Result(0, "ok", data);
    }
    public static Result ok() {
        return new Result(0, "ok", null);
    }
    public static Result error(String msg) {
        return new Result(10001, msg, null);
    }
}
