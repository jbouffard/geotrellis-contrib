package geotrellis.contrib.vlm.gdal

import java.net.URI

import org.scalatest._


class VSIPathSpec extends FunSpec with Matchers {
  describe("Formatting the given uris") {
    val fileName = "file-1.tiff"

    describe("http") {
      it("http url") {
        val filePath = "www.radomdata.com/test-files/file-1.tiff"
        val url = s"http://$filePath"
        val expectedPath = s"/vsicurl/$url"
        val vsi = VSIPath2(url)

        VSIPath2(url).vsiPath should be (expectedPath)
      }

      it("http that points to gzip url") {
        val filePath = "www.radomdata.com/test-files/data.gzip"
        val url = s"http://$filePath"
        val expectedPath = s"/vsigzip//vsicurl/$url"
        val vsi = VSIPath2(url)

        VSIPath2(url).vsiPath should be (expectedPath)
      }

      it("http that points to gzip with ! url") {
        val filePath = "www.radomdata.com/test-files/data.gzip"
        val url = s"http://$filePath!$fileName"
        val expectedPath = s"/vsigzip//vsicurl/http://$filePath/$fileName"
        val vsi = VSIPath2(url)

        VSIPath2(url).vsiPath should be (expectedPath)
      }

      it("zip+http url") {
        val filePath = "www.radomdata.com/test-files/data.zip"
        val url = s"zip+http://$filePath"
        val expectedPath = s"/vsizip//vsicurl/$url"
        val vsi = VSIPath2(url)

        VSIPath2(url).vsiPath should be (expectedPath)
      }

      it("zip+http with ! url") {
        val filePath = "www.radomdata.com/test-files/data.zip"
        val url = s"zip+http://$filePath!$fileName"
        val expectedPath = s"/vsizip//vsicurl/http://$filePath/$fileName"
        val vsi = VSIPath2(url)

        VSIPath2(url).vsiPath should be (expectedPath)
      }
    }

    describe("file") {
      it("file uri") {
        val filePath = "/home/jake/Documents/test-files/file-1.tiff"
        val uri = s"file://$filePath"
        val expectedPath = filePath

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("file that points to zip uri") {
        val filePath = "/home/jake/Documents/test-files/files.zip"
        val uri = s"file://$filePath"
        val expectedPath = s"/vsizip/$filePath"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("file that points to zip with ! uri") {
        val filePath = "/home/jake/Documents/test-files/files.zip"
        val uri = s"file://$filePath!$fileName"
        val expectedPath = s"/vsizip/$filePath/$fileName"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("zip+file uri") {
        val path = "/tmp/some/data/data.zip"
        val uri = s"zip+file://$path"
        val expectedPath = "/vsizip//tmp/some/data/data.zip"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("zip+file with ! uri") {
        val path = "/tmp/some/data/data.zip"
        val uri = s"zip+file://$path!$fileName"
        val expectedPath = s"/vsizip/$path/$fileName"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }
    }

    describe("s3") {
      it("s3 uri") {
        val filePath = "test-files/nlcd/data/tiff-0.tiff"
        val uri = s"s3://$filePath"
        val expectedPath = s"/vsis3/$filePath"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("s3 that points to gzip uri") {
        val filePath = "test-files/nlcd/data/data.gzip"
        val uri = s"s3://$filePath"
        val expectedPath = s"/vsigzip//vsis3/$filePath"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("s3 that points to gzip with uri") {
        val filePath = "test-files/nlcd/data/data.gzip"
        val uri = s"s3://$filePath!$fileName"
        val expectedPath = s"/vsigzip//vsis3/$filePath/$fileName"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("gzip+s3 uri") {
        val path = "some/bucket/data/data.gzip"
        val uri = s"gzip+s3://$path"
        val expectedPath = s"/vsigzip//vsis3/$path"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("gzip+s3 uri with !") {
        val path = "some/bucket/data/data.gzip"
        val uri = s"gzip+s3://$path!$fileName"
        val expectedPath = s"/vsigzip//vsis3/$path/$fileName"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }
    }

    describe("hdfs") {
      it("hdfs uri") {
        val filePath = "test-files/nlcd/data/tiff-0.tiff"
        val uri = s"hdfs://$filePath"
        val expectedPath = s"/vsihdfs/$uri"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("hdfs that points to tgz uri") {
        val filePath = "test-files/nlcd/data/my_data.tgz"
        val uri = s"hdfs://$filePath"
        val expectedPath = s"/vsitar//vsihdfs/$uri"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("hdfs that points to tgz with ! uri") {
        val filePath = "test-files/nlcd/data/my_data.tgz"
        val uri = s"hdfs://$filePath!$fileName"
        val expectedPath = s"/vsitar//vsihdfs/$filePath/$fileName"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("zip+hdfs uri") {
        val filePath = "test-files/nlcd/data/data.zip"
        val uri = s"zip+hdfs://$filePath"
        val expectedPath = s"/vsizip//vsihdfs/$uri"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("zip+hdfs with ! uri") {
        val filePath = "test-files/nlcd/data/data.zip"
        val uri = s"zip+hdfs://$filePath!$fileName"
        val expectedPath = s"/vsizip//vsihdfs/$filePath/$fileName"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }
    }

    describe("Google Cloud Storage") {
      it("Google Cloud Storage uri") {
        val filePath = "test-files/nlcd/data/tiff-0.tiff"
        val uri = s"gs://$filePath"
        val expectedPath = s"/vsigs/$filePath"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("Google Cloud Storage that points to tar uri") {
        val filePath = "test-files/nlcd/data/data.tar"
        val uri = s"gs://$filePath"
        val expectedPath = s"/vsitar//vsigs/$filePath"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("Google Cloud Storage that points to tar with ! uri") {
        val filePath = "test-files/nlcd/data/data.tar"
        val uri = s"gs://$filePath!$fileName"
        val expectedPath = s"/vsitar//vsigs/$filePath/$fileName"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("tar+gs uri") {
        val filePath = "test-files/nlcd/data/data.tar"
        val uri = s"tar+gs://$filePath"
        val expectedPath = s"/vsitar//vsigs/$filePath"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("tar+gs with ! uri") {
        val filePath = "test-files/nlcd/data/data.tar"
        val uri = s"tar+gs://$filePath!$fileName"
        val expectedPath = s"/vsitar//vsigs/$filePath/$fileName"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }
    }

    describe("Azure") {
      it("Azure uri") {
        val uri = "wasb://test-files@myaccount.blah.core.net/nlcd/data/tiff-0.tiff"
        val expectedPath = "/vsiaz/test-files/nlcd/data/tiff-0.tiff"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("Azure that points to kmz uri") {
        val uri = "wasb://test-files@myaccount.blah.core.net/nlcd/data/info.kmz"
        val expectedPath = "/vsizip//vsiaz/test-files/nlcd/data/info.kmz"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("Azure that points to kmz with ! uri") {
        val uri = s"wasb://test-files@myaccount.blah.core.net/nlcd/data/info.kmz!$fileName"
        val expectedPath = s"/vsizip//vsiaz/test-files/nlcd/data/info.kmz/$fileName"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("wasb+zip uri") {
        val uri = "zip+wasb://test-files@myaccount.blah.core.net/nlcd/data/info.zip"
        val expectedPath = "/vsizip//vsiaz/test-files/nlcd/data/info.zip"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }

      it("wasb+zip with ! uri") {
        val path = "zip+wasb://test-files@myaccount.blah.core.net/nlcd/data/info.zip"
        val uri = s"$path!$fileName"
        val expectedPath = s"/vsizip//vsiaz/test-files/nlcd/data/info.zip/$fileName"

        VSIPath2(uri).vsiPath should be (expectedPath)
      }
    }
  }
}
