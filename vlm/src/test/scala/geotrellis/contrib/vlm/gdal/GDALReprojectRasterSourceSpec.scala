/*
 * Copyright 2018 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.contrib.vlm.gdal

import geotrellis.contrib.vlm._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.raster.testkit._

import org.scalatest._

import java.io.File

// TODO: fix test, compare to GDAL WARP results
class GDALReprojectRasterSourceSpec extends FunSpec with RasterMatchers with BetterRasterMatchers with GivenWhenThen {

  /**
    * gdalwarp -of VRT -r bilinear -et 0.125 -tap -tr 1.1205739005262034E-4 1.1205739005262034E-4 -s_srs '+proj=lcc +lat_1=36.16666666666666 +lat_2=34.33333333333334 +lat_0=33.75 +lon_0=-79 +x_0=609601.22 +y_0=0 +datum=NAD83 +units=m +no_defs'  -t_srs '+proj=longlat +datum=WGS84 +no_defs'
    *
    * gdal_translate in.vrt out.tif
    *
    * */

  describe("Reprojecting a RasterSource") {
    val uri = s"${new File("").getAbsolutePath()}/src/test/resources/img/aspect-tiled.tif"

    val expectedUri = Map[ResampleMethod, String](
      Bilinear -> s"${new File("").getAbsolutePath()}/src/test/resources/img/aspect-tiled-bilinear.tif",
      NearestNeighbor -> s"${new File("").getAbsolutePath()}/src/test/resources/img/aspect-tiled-near.tif"
    )

    def testReprojection(method: ResampleMethod) = {
      val rasterSource = GDALRasterSource(uri)
      val expectedRasterSource = GDALRasterSource(expectedUri(method))
      val expectedRasterExtent = expectedRasterSource.rasterExtent
      val warpRasterSource = rasterSource.reprojectToRegion(LatLng, expectedRasterExtent, method)
      val testBounds = GridBounds(0, 0, expectedRasterExtent.cols, expectedRasterExtent.rows).split(64,64).toSeq

      for (bound <- testBounds) yield {
        withClue(s"Read window ${bound}: ") {
          val targetExtent = expectedRasterExtent.extentFor(bound)
          val testRasterExtent = RasterExtent(
            extent     = targetExtent,
            cellwidth  = expectedRasterExtent.cellwidth,
            cellheight = expectedRasterExtent.cellheight,
            cols       = bound.width,
            rows       = bound.height
          )

          val expected = expectedRasterSource.read(testRasterExtent.extent).get

          val actual = warpRasterSource.read(bound).get

          actual.extent.covers(expected.extent) should be (true) // -- doesn't work due to a precision issue
          actual.rasterExtent.extent.xmin should be (expected.rasterExtent.extent.xmin +- 1e-5)
          actual.rasterExtent.extent.ymax should be (expected.rasterExtent.extent.ymax +- 1e-5)
          actual.rasterExtent.cellwidth should be (expected.rasterExtent.cellwidth +- 1e-5)
          actual.rasterExtent.cellheight should be (expected.rasterExtent.cellheight +- 1e-5)

          withGeoTiffClue(actual, expected, LatLng)  {
            assertRastersEqual(actual, expected)
          }
        }
      }


    }

    it("should reproject using NearestNeighbor") {
      testReprojection(NearestNeighbor)
    }

    it("should reproject using Bilinear") {
      testReprojection(Bilinear)
    }
  }
}
