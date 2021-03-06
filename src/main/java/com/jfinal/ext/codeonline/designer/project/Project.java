package com.jfinal.ext.codeonline.designer.project;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jfinal.ext.codeonline.common.AntKit;
import com.jfinal.ext.codeonline.common.ScriptKit;
import com.jfinal.ext.codeonline.common.TaskKit;
import com.jfinal.ext.codeonline.core.Config;
import com.jfinal.ext.codeonline.metadata.impl.db.model.DbInfo;
import com.jfinal.ext.codeonline.designer.entity.Entity;
import com.jfinal.ext.codeonline.designer.group.Groups;
import com.jfinal.ext.codeonline.designer.task.Task;
import com.jfinal.ext.codeonline.designer.task.TaskParam;
import com.jfinal.ext.kit.ModelExt;
import com.jfinal.kit.StrKit;
import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;

import java.io.File;
import java.util.List;
import java.util.Map;

import static com.jfinal.ext.codeonline.core.Constants.FS;

@SuppressWarnings("serial")
public class Project extends ModelExt<Project> {

    private static final Logger LOG = Logger.getLogger(Project.class);

    public static final Project DAO = new Project();

    public List<Project> findAll() {
        return find("select * from project");
    }

    public List<Entity> getEntities() {
        return Entity.DAO.find("select * from entity where project_id = ?", get("id"));
    }

    public Page<Project> page(int pageNumber, int pageSize) {
        return paginate(pageNumber, pageSize, "select *", "from project order by id asc");
    }

    public List<Record> eval(Task task) {
        List<Record> results = Lists.newArrayList();
        List<Entity> entities = this.getEntities();
        String taskType = task.getStr("tasktype");
        Map<String, Object> root = Maps.newHashMap();
        root.put("project", this);
        root.put("tables", entities);
        root.put("outtype", task.getStr("outtype"));
        root.put("TaskKit", TaskKit.class);
        root.put("DriverInfo", DbInfo.class);
//        root.put("ViewType", ViewEngine.class);
        if ("single".equals(taskType)) {
            exec2(task, results, root);
        } else if ("multipal".equals(taskType)) {
            for (Entity entity : entities) {
                root.put("entity", entity);
                exec2(task, results, root);
            }
        }
        return results;
    }

    private void exec2(Task task, List<Record> results, Map<String, Object> root) {
        Record result = new Record();
        File folderDir = new File(ScriptKit.exec(task.getStr("folder"), root).toString());
        if (!folderDir.exists()) {
            folderDir.mkdirs();
        }
        List<TaskParam> params = task.getTaskParams();
        for (int i = 0; i < params.size(); i++) {
            TaskParam param = params.get(i);
            if (StrKit.isBlank(param.getStr("key"))) continue;
            String value = param.getStr("expression");
            result.set("param" + (i + 1), ScriptKit.exec(value, root).toString());
        }
        String templatepath = ScriptKit.exec(task.getStr("templatepath"), root);
        String floder = ScriptKit.exec(task.getStr("folder"), root);
        String filename = ScriptKit.exec(task.getStr("filename"), root);

        result.set("templatepath", templatepath);
        result.set("folder", floder);
        result.set("filename", filename);
        results.add(result);
    }


    public List<String> runGroup(Groups group) {
        List<String> paths = Lists.newArrayList();
        for (Task task : group.tasks()) {
            paths.addAll(TaskKit.processTask(this, task));
        }
        return paths;
    }

    public void start() {
        String rootDir = Config.getTargetPath() + FS + get("name");
        String sqlFilePath = rootDir + FS + "sql" + FS + get("name") + ".sql";
        AntKit.sqlScript(Config.getConfigDataProvider().findDriver(getStr("dbType")),
                getStr("jdbcurl"), getStr("username"), getStr("password"), sqlFilePath);

    }

    public Groups getGroups() {
        return Groups.DAO.findById(getInt("groups_id"));
    }
}
