package tw.com;

public class ProjectAndEnv {
	
	private String project;
	private String env;

	public String getProject() {
		return project;
	}

	public String getEnv() {
		return env;
	}

	public ProjectAndEnv(String project, String env) {
		this.project = project;
		this.env = env;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((env == null) ? 0 : env.hashCode());
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProjectAndEnv other = (ProjectAndEnv) obj;
		if (env == null) {
			if (other.env != null)
				return false;
		} else if (!env.equals(other.env))
			return false;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ProjectAndEnv [project=" + project + ", env=" + env + "]";
	}
	
	

}