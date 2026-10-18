package com.group15.lab1.repository;

import com.group15.lab1.entity.TaskOperationLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskOperationLogRepository extends JpaRepository<TaskOperationLog, Long> {

    List<TaskOperationLog> findByTaskIdOrderByCreatedAtDescIdDesc(Long taskId);
}
