<xsl:stylesheet 
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
   xmlns:xalan="http://xml.apache.org/xslt" 
   version="1.0">

   <xsl:output method="xml" indent="yes" encoding="UTF-8"  xalan:indent-amount="3"/>

   <xsl:template match="@*|node()">
      <xsl:copy>
         <xsl:apply-templates select="@*|node()" />
      </xsl:copy>
   </xsl:template>

</xsl:stylesheet>