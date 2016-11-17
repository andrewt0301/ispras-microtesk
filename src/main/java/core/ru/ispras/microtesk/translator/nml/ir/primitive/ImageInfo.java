/*
 * Copyright 2016 ISP RAS (http://www.ispras.ru)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package ru.ispras.microtesk.translator.nml.ir.primitive;

import ru.ispras.fortress.util.InvariantChecks;

public final class ImageInfo {
  private final int maxImageSize;
  private final boolean imageSizeFixed;

  public ImageInfo(final int maxImageSize, final boolean imageSizeFixed) {
    InvariantChecks.checkGreaterOrEqZero(maxImageSize);

    this.maxImageSize = maxImageSize;
    this.imageSizeFixed = imageSizeFixed;
  }

  public int getMaxImageSize() {
    return maxImageSize;
  }

  public boolean isImageSizeFixed() {
    return imageSizeFixed;
  }

  public ImageInfo or(final ImageInfo other) {
    InvariantChecks.checkNotNull(other);
    return new ImageInfo(
        Math.max(this.maxImageSize, other.maxImageSize),
        this.maxImageSize == other.maxImageSize && this.imageSizeFixed && other.imageSizeFixed
        );
  }

  @Override
  public String toString() {
    return String.format(
        "ImageInfo [maxImageSize=%s, imageSizeFixed=%s]",
        maxImageSize,
        imageSizeFixed
        );
  }
}
