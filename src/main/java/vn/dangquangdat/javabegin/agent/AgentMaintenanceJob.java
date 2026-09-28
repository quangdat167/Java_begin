package vn.dangquangdat.javabegin.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Vi du scheduler nhe; khong tu dong chay agent de tranh side effect khi hoc. */
@Component
public class AgentMaintenanceJob {
    private static final Logger log = LoggerFactory.getLogger(AgentMaintenanceJob.class);

    @Scheduled(fixedDelayString = "PT1M")
    void heartbeat() {
        log.debug("Agent scheduler heartbeat");
    }
}
