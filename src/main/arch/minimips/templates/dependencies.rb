#
# Copyright 2015 ISP RAS (http://www.ispras.ru)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

require_relative 'minimips_base'

#
# Description:
#
# This test template demonstrates how MicroTESK handles data dependencies between
# instructions when generating test data. Data generated for previous instructions
# in the same instruction sequence is taken into account (reused) when data is being
# generated for an instruction accessing the same resource (e.g. register).
#
class DependenciesTemplate < MiniMipsBaseTemplate

  def initialize
    super
    # Random seed initialization.
    @random_seed = 0
  end

  def run
    int32_dist = dist(range(:value=> [0xDEADBEEF, 0xBADF00D], :bias => 10),
                      range(:value => 0..31,                  :bias => 50),
                      range(:value => 0xffffFF00..0xffffFFFF, :bias => 40))

    5.times {
      atomic {
        add t0, t1,   t2 do situation('add.overflow') end
        add t0, t1,   t3 do situation('add.normal') end
        add t0, t1,   t4 do situation('add.overflow') end
        add t0, zero, t5 do situation('random_biased', :size => 32, :dist => int32_dist) end
        add t0, t5,   t6 do situation('add.normal') end
      }
    }
  end
end
