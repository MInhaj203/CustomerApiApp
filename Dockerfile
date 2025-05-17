# Use official Tomcat base image
FROM tomcat:10.1-jdk17

# Remove default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy WAR file into webapps folder
COPY target/customerapiapp.war /usr/local/tomcat/webapps/ROOT.war

# Expose port
EXPOSE 8080

# Start Tomcat (this is default, but we include it for clarity)
CMD ["catalina.sh", "run"]
