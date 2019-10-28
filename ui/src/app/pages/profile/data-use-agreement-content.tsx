import {BolderHeader, BoldHeader} from 'app/components/headers';
import * as React from 'react';
import {
  AoUTitle,
  IndentedListItem,
  IndentedOrderedList,
  IndentedUnorderedList,
  SecondHeader
} from './data-use-agreement-styles';

const CORE_VALUES_URL = 'https://allofus.nih.gov/about/core-values';
const PII_URL = 'https://www.govinfo.gov/content/pkg/CFR-2014-title2-vol1/pdf/CFR-2014-title2-vol1-sec200-79.pdf';
const MARKETING_URL =
  'https://www.govinfo.gov/content/pkg/CFR-2004-title45-vol1/pdf/CFR-2004-title45-vol1-sec164-501.pdf';

{/* NOTE: Make sure to update dataUseAgreementVersion if there is any change to the DUA text. */}
export const DataUseAgreementContent = () => {
  return <div>
    <BolderHeader style={{display: 'flex', justifyContent: 'center'}}><AoUTitle/></BolderHeader>
    <BoldHeader  style={{display: 'flex', justifyContent: 'center'}}>Demonstration Project Data Use Agreement</BoldHeader>
    <p>
      This data use agreement describes how <AoUTitle/> data can and cannot be used for the purposes
      of program-approved demonstration projects.
    </p>
    <p>
      This is an agreement between Vanderbilt University Medical Center and authorized demonstration users of data
      from
      the <AoUTitle/>.
    </p>
    <p>
      An <strong>authorized demonstration user</strong> is a person who is authorized to access
      and/or work with registered or
      controlled tier data from the <AoUTitle/> for the exclusive purpose of a program-approved
      demonstration project. Authorized demonstration users are limited to trainees, faculty or staff
      at <AoUTitle/> consortium partner institutions.
    </p>
    <p>
      <strong>Before</strong> they access and/or work with <AoUTitle/> data, authorized demonstration users must:
      <IndentedOrderedList>
        <li>complete the <AoUTitle/> research ethics training; and</li>
        <li>read and attest to this data use agreement</li>
      </IndentedOrderedList>
    </p>
    <p>Please read this agreement carefully and completely before signing.</p>
    <SecondHeader>As an “Authorized Demonstration User” of the <AoUTitle/> data, I
      will:</SecondHeader>
    <IndentedUnorderedList>
      <li>read and adhere to the <AoUTitle/> <a target='_blank' href={CORE_VALUES_URL}>core values</a>.
      </li>
      <li>know and follow all laws regarding research involving human data and data privacy that are
        applicable in the area where I am conducting research.
        <IndentedUnorderedList>
          <IndentedListItem>In the US, this includes all applicable federal, state, and local laws.</IndentedListItem>
          <IndentedListItem>Outside of the US, other laws will apply.</IndentedListItem>
        </IndentedUnorderedList>
      </li>
      <li>respect the privacy of research participants at all times.
        <IndentedUnorderedList>
          <IndentedListItem>
            I will <strong>NOT</strong> use or disclose any
            information that directly identifies one or more participants.
            <IndentedUnorderedList>
              <IndentedListItem>
                If I become aware of any information that directly identifies one or more
                participants, I will notify the <AoUTitle/> immediately
                using the automatic notification system.
              </IndentedListItem>
            </IndentedUnorderedList>
          </IndentedListItem>
          <IndentedListItem>
            I will <strong>NOT</strong> attempt to re-identify research
            participants or their relatives.
            <IndentedUnorderedList>
              <IndentedListItem>
                If I unintentionally re-identify participants through the process of my work, I will
                contact the <AoUTitle/> immediately using the appropriate process.
              </IndentedListItem>
            </IndentedUnorderedList>
          </IndentedListItem>
          <IndentedListItem>
            If I become aware of any uses or disclosures of <AoUTitle/> data that
            could endanger the security or privacy of research participants, I will contact
            the <AoUTitle/> immediately using the appropriate process.
          </IndentedListItem>
        </IndentedUnorderedList>
        <IndentedListItem>provide a meaningful description of my research purpose when establishing
          my <AoUTitle/> workspace.</IndentedListItem>
        <IndentedUnorderedList>
          <IndentedListItem>This description will accurately reflect the demonstration project proposal for which I
            received <i>All of Us</i> approval.</IndentedListItem>
          <IndentedListItem>Within my workspace, I will only use the data for the demonstration project for which I have
            received <i>All of Us</i> approval.
          </IndentedListItem>
        </IndentedUnorderedList>
        <IndentedListItem>take full responsibility for any external data, files, or software that I import into my
          Workspace and the consequences thereof.</IndentedListItem>
        <IndentedUnorderedList>
          <IndentedListItem>I will know and follow all applicable laws, regulations, and policies regarding access and
            use for any external data, files, or software that I upload into my Workspace.</IndentedListItem>
          <IndentedListItem>I will <strong>NOT</strong> upload data or files containing
            personally identifiable information (PII).
            <IndentedUnorderedList><IndentedListItem>
              PII means information that can be used to distinguish or trace the identity of an individual
              (e.g., name, social security number, biometric records, etc.) either alone, or when combined with other
              personal or identifying information that is linked or linkable to a specific
              individual <a href={PII_URL}>(2 CFR §200.79)</a>
            </IndentedListItem></IndentedUnorderedList>
          </IndentedListItem>
          <IndentedListItem>I will use any external data, files, or software that I upload into my Workspace
            exclusively for the research purpose I have provided for that Workspace.</IndentedListItem>
          <IndentedListItem>I will <strong>NOT</strong> use any external data, files, or software that I upload into
            my Workspace for any malicious purposes.</IndentedListItem>
          <IndentedListItem>If any import of data, files, or software into my Workspace results in unforeseen
            consequences and/or unintentional violation of these terms, I will notify
            the <AoUTitle/> as soon as I become aware using the appropriate process.</IndentedListItem>
        </IndentedUnorderedList>
        <li>use a version of the <AoUTitle/> database that is current at or after the time my
          analysis begins.</li>
        <IndentedUnorderedList>
          <IndentedListItem>Archived versions of the database are maintained for the sole purpose of completion of
            existing studies or replication of previous studies. New work may not be initiated on
            archived versions of the database.
          </IndentedListItem>
        </IndentedUnorderedList>
      </li>
      <li>share the results of my demonstration project and all contents of my Workbench with the <AoUTitle/>. </li>
      <IndentedUnorderedList>
        <li>My workbench and its contents may be made public for the benefit of all authorized users.</li>
      </IndentedUnorderedList>
      <li>honor the contribution to my work of those who take part in <i>All of Us</i>
        <IndentedUnorderedList>
          <li>I will acknowledge the <AoUTitle/> and its research participants
            in all oral and written presentations, disclosures, and publications resulting from
            any analyses of the data.
            <IndentedUnorderedList>
              <li style={{margin: '0.5rem 0'}}>Here is an example acknowledgement statement:
                <br/>
                “The <AoUTitle/> is supported by the National Institutes of Health, Office of the
                Director: Regional Medical Centers: 1 OT2 OD026549; 1 OT2 OD026554; 1 OT2 OD026557; 1 OT2 OD026556;
                1 OT2 OD026550; 1 OT2 OD 026552; 1 OT2 OD026553; 1 OT2 OD026548; 1 OT2 OD026551; 1 OT2 OD026555;
                IAA #: AOD 16037; Federally Qualified Health Centers: HHSN 263201600085U; Data and Research Center:
                5 U2C OD023196; Biobank: 1 U24 OD023121; The Participant Center: U24 OD023176; Participant Technology
                Systems Center: 1 U24 OD023163; Communications and Engagement: 3 OT2 OD023205; 3 OT2 OD023206;
                and Community Partners: 1 OT2 OD025277; 3 OT2 OD025315; 1 OT2 OD025337; 1 OT2 OD025276. In addition,
                the <AoUTitle/> would not be possible without the partnership of its participants.”
              </li>
              <li>
                I will submit an electronic version of a final, peer-reviewed manuscript to PubMed Central
                immediately upon acceptance for publication, to be made publicly available no later than 12
                months after the official date of publication.
              </li>
            </IndentedUnorderedList>
          </li>
        </IndentedUnorderedList>
      </li>
    </IndentedUnorderedList>
    <SecondHeader>As “Authorized Demonstration User” of the <AoUTitle/> data, I
      will:</SecondHeader>
    <IndentedUnorderedList>
      <li><strong>NOT</strong> share my login information with anyone.
        <IndentedUnorderedList>
          <li>I will not share my login information with another authorized demonstration or other user
            of the <AoUTitle/>.
          </li>
          <li>I will not create any group or shared accounts.</li>
        </IndentedUnorderedList>
      </li>
      <li>
        <strong>NOT</strong> use <AoUTitle/> data or any external data, files, or software that I upload
        into the Research Workbench for research that is discriminatory or stigmatizing of individuals, families,
        groups, or communities. Please review the All of Us policy on stigmatizing research here.
        <IndentedUnorderedList>
          <li>I will contact the <AoUTitle/> Resource Access Board (RAB) for
            further guidance on this point as needed.
          </li>
        </IndentedUnorderedList>
      </li>
      <li><strong>NOT</strong> attempt to contact <AoUTitle/> participants.
      </li>
      <li><strong>NOT</strong> make copies of or download participant-level data and remove it from
        the <AoUTitle/> environment.
        <IndentedUnorderedList>
          <li>I will not take screenshots or attempt any other way of
            copying participant-level data.
          </li>
        </IndentedUnorderedList>
      </li>
      <li><strong>NOT</strong> redistribute or publish registered or controlled tier <AoUTitle/> data including
        aggregate statistics that are more granular than buckets of 20 individuals without explicit approval from
        the <AoUTitle/>.
      </li>
      <li><strong>NOT</strong> sell or distribute <AoUTitle/> data at any level of granularity for the purpose
      of profit or monetary gains.</li>
      <li><strong>NOT</strong> attempt to link participant-level <i>All of Us</i> data from the registered or controlled
        tier with participant-level data from other sources without explicit permission from the <AoUTitle/>.</li>
      <li><strong>NOT</strong> use <AoUTitle/> data or any part of the Research Hub for marketing purposes.
        <IndentedUnorderedList>
          <li>“Marketing” means a communication about a product or service that encourages recipients of the
            communication to purchase or use the product or service (<a href={MARKETING_URL}>US 45 CFR 164.501</a>).
          </li>
        </IndentedUnorderedList>
      </li>
      <li><strong>NOT</strong> represent that the <AoUTitle/> endorses or approves of my research unless such
        endorsement is expressly provided.
      </li>
    </IndentedUnorderedList>

    <SecondHeader>Data Disclaimer:</SecondHeader>
    <p>The <AoUTitle/> does not guarantee the accuracy of the data in
      the <AoUTitle/> database. The <AoUTitle/> does
      not guarantee the performance of the software in the <AoUTitle/> database.
      The <AoUTitle/> does not warrant or endorse
      the research results obtained by using the <i>All of Us</i> database.
    </p>
  </div>;
};
