#
# Copyright 2019 ISP RAS (http://www.ispras.ru)
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
# This test template demonstrates how to randomly allocate registers so
# that they do not conflict with other registers used in the test case.
#
class RegisterAllocationTemplate < MiniMipsBaseTemplate

  def pre
    super

    #
    # Test case level epilogue to return from an exception.
    #
    epilogue {
      nop
    }

    #
    # Start address
    #
    org 0x00020000
  end

  def run
    block(:combinator => 'random', :compositor => 'random') {
      
      sequence {
        constraint {
          allocation(
            'reg',
            :retain  => [ t0, t1, t2, t3, t4, t5, t6, t7, t8, t9 ],
            :track   => 2,
            :read    => { :read => 20, :write => 30, :free => 50 },
            :write   => { :read => 50, :write => 0,  :free => 50 }
          )
        }

        add reg(_), reg(_), reg(_)
        add reg(_), reg(_), reg(_)
        add reg(_), reg(_), reg(_)
      }

      sequence {
        constraint {
          allocation(
            'reg',
            :exclude => [ zero, at, k0, k1, gp, sp, fp, ra ],
            :track   => 2,
            :used    => { :used => 50, :free => 50 }
          )
        }

        sub reg(_), reg(_), reg(_)
        sub reg(_), reg(_), reg(_)
        sub reg(_), reg(_), reg(_)
      }
    }.run 3
  end

end
