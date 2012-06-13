package de.banapple.maven.antlr;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Arrays;

import org.antlr.works.Console;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which generates syntax diagrams from a grammar.
 * The diagram image files are then referenced in an index html file.
 *
 * @goal generate
 * 
 * @phase site
 */
public class SyntaxDiagramMojo
    extends AbstractMojo
{
    /**
     * Output directory for the generated images.
     * 
     * @parameter expression="${project.build.directory}/antlrdiagrams"
     * @required
     */
    private File outputDirectory;

    /**
     * The grammar file for which syntax diagrams should be created.
     * 
     * @parameter
     * @required
     */
    private File grammarFile;
    
    public void execute()
        throws MojoExecutionException
    {
        getLog().info("outputDirectory: " + outputDirectory);
        getLog().info("grammarFile: " + grammarFile);

        /* create output directory if it not exists */
        File f = outputDirectory;
        if ( !f.exists() )
        {
            f.mkdirs();
        }

        /* create the rule images using antlrworks */
        String[] args = new String[] {
                "-f", grammarFile.getAbsolutePath(),
                "-o", outputDirectory.getAbsolutePath(),
                "-sd", "png"
        };
        try {
            getLog().info("generate diagrams using antlrworks with arguments: "
                    + StringUtils.join(args," "));
            Console.main(args);
        } catch (Exception e) {
            getLog().error("failed to generate syntax diagrams", e);
            throw new RuntimeException(e);
        }
        
        /* create an index file */
        createIndex();
    }

    /**
     * Creates an index file in the output directory containing all
     * generated images.
     */
    private void createIndex()
    {
        /* retrieve all png files */
        String[] imageFilenames = outputDirectory.list(new FilenameFilter() {            
            public boolean accept(File dir, String filename)
            {
                return filename.endsWith("png");
            }
        });
        
        /* sort filenames by lower case first */
        Arrays.sort(imageFilenames);
        
        StringBuilder content = new StringBuilder();
        content.append("<html>");
        content.append("<body>");
        
        /* table of contents */
        content.append("<a name=\"top\" />");
        content.append("<ol>");
        for (String filename : imageFilenames) {
            String name = filename.substring(0, filename.length()-4);
            content.append("<li>")
                .append("<a href=\"#")
                .append(name)
                .append("\">")
                .append(name)
                .append("</a>")
                .append("</li>");
        }
        content.append("</ol>");
        
        /* images */
        for (String filename : imageFilenames) {
            String name = filename.substring(0, filename.length()-4); 
            content.append("<h2>")
                .append(name)
                .append("</h2>");
            content.append("<a name=\"").append(name).append("\" />");
            content.append("<img src=\"").append(filename).append("\" />");            
            content.append("<br/><a href=\"#top\">up</a>");
        }
        content.append("</body>");
        content.append("</html>");
        
        File indexFile = new File(outputDirectory, "index.html");
        try {
            IOUtils.write(content.toString(), new FileOutputStream(indexFile));
        } catch (Exception e) {
            getLog().error("failed to generate index file", e);
            throw new RuntimeException(e);
        }
    }
}
