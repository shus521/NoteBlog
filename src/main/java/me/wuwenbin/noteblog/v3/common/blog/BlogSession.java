package me.wuwenbin.noteblog.v3.common.blog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.wuwenbin.modules.utils.http.WebUtils;
import me.wuwenbin.modules.utils.lang.LangUtils;
import me.wuwenbin.noteblog.v3.model.User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.time.LocalDateTime;

import static java.lang.Boolean.FALSE;
import static java.time.LocalDateTime.now;

/**
 * created by Wuwenbin on 2018/2/7 at 20:56
 */
@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlogSession implements Serializable {

    @Builder.Default
    private String id = LangUtils.random.uuidCool();
    private String host;
    @Builder.Default
    private LocalDateTime startTimestamp = now();
    @Builder.Default
    private LocalDateTime lastAccessTime = now();
    @Builder.Default
    private long timeout = DEFAULT_TIMEOUT_MILLS;
    @Builder.Default
    private boolean expired = FALSE;
    private User sessionUser;

    public static final long DEFAULT_TIMEOUT_MILLS = 30 * 60 * 1000;

    private static HttpServletRequest getRequest() {
        ServletRequestAttributes ra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return ra.getRequest();
    }

    /**
     * 更新session
     */
    public void update() {
        String info = "update session for id:[{}], at [{}] by url:[{}] with ip:[{}]";
        log.info(LangUtils.string.placeholder(info, this.getId(), LocalDateTime.now(), getRequest().getRequestURL()), WebUtils.getRemoteAddr(getRequest()));
        this.lastAccessTime = now();
        if (!host.equals(WebUtils.getRemoteAddr(getRequest()))) {
            log.info("ip变动，存在非法访问情况");
            this.expired = true;
        }
    }

    /**
     * 注销session，即把session变为过期状态
     */
    public void destroy() {
        String info = "destroy session for id:[{}], at [{}]";
        log.info(LangUtils.string.placeholder(info, this.getId(), LocalDateTime.now()));
        this.expired = true;
    }


    public boolean isExpired() {
        if (this.expired) {
            return true;
        }

        long timeout = getTimeout();
        if (timeout >= 0) {
            LocalDateTime lastAccessTime = getLastAccessTime();
            if (lastAccessTime == null) {
                throw new IllegalStateException("最后访问时间为空");
            }
            LocalDateTime start = getStartTimestamp();
            LocalDateTime expire = start.plusSeconds(timeout / 1000);
            boolean isExpire = lastAccessTime.isAfter(expire);
            if (isExpire) {
                this.expired = true;
            }
            return isExpire;
        }
        this.expired = true;
        return false;
    }
}